package com.inu.jeongbobada.global.exception.code;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// #115·#119: 에러 code 규칙을 CI에서 강제한다. DB가 필요 없는 단위 테스트.
//   1. 상황 하나에 code 하나 — 모든 ErrorCode enum을 통틀어 code가 겹치지 않는다 (USER_409 3종 같은 재발 방지)
//   2. code는 의미 있는 UPPER_SNAKE_CASE 문자열 — 숫자 금지 (USER_409, COURSE_4041 형식 재발 방지)
//   3. docs/api-spec.md의 "에러 코드" 표와 코드가 일치한다 — code와 HTTP 상태가 같고, 빠지거나 남는 줄이 없다
// ErrorCode enum은 BaseErrorCode 구현체를 패키지 스캔으로 찾으므로, 새 도메인 enum도 자동으로 검사된다.
class ErrorCodeRulesTest {

    private static final Pattern CODE_FORMAT = Pattern.compile("^[A-Z]+(_[A-Z]+)*$");
    // "| 404 | `USER_NOT_FOUND` | 설명 |"
    private static final Pattern DOC_ROW = Pattern.compile("^\\|\\s*(\\d{3})\\s*\\|\\s*`([^`]+)`\\s*\\|", Pattern.MULTILINE);
    private static final Path API_SPEC = Path.of("docs/api-spec.md");

    private static List<BaseErrorCode> allCodes;

    @BeforeAll
    static void scanErrorCodes() throws ClassNotFoundException {
        // 기본 스캐너는 enum을 후보로 보지 않을 수 있어서, 구체 클래스면 모두 후보로 받는다
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isConcrete();
            }
        };
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseErrorCode.class));

        allCodes = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.inu.jeongbobada")) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            if (type.isEnum()) {
                for (Object constant : type.getEnumConstants()) {
                    allCodes.add((BaseErrorCode) constant);
                }
            }
        }
    }

    @Test
    void ErrorCode_enum을_스캔으로_찾는다() {
        // 0개면 스캔이 깨진 것이라 아래 테스트들이 아무것도 검사하지 않고 통과해버린다
        assertThat(allCodes)
            .extracting(code -> code.getClass().getSimpleName())
            .contains("GlobalErrorCode", "UserErrorCode", "ProfessorErrorCode", "CourseErrorCode", "ProfessorCommentErrorCode");
    }

    @Test
    void code는_모든_enum을_통틀어_겹치지_않는다() {
        Map<String, List<String>> owners = allCodes.stream()
            .collect(Collectors.groupingBy(BaseErrorCode::getCode, LinkedHashMap::new,
                Collectors.mapping(ErrorCodeRulesTest::name, Collectors.toList())));

        Map<String, List<String>> duplicated = owners.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(duplicated).as("같은 code를 여러 상황이 쓰고 있음 — 상황마다 다른 code를 줄 것 (#115)").isEmpty();
    }

    @Test
    void code는_숫자_없는_UPPER_SNAKE_CASE다() {
        List<String> invalid = allCodes.stream()
            .filter(code -> !CODE_FORMAT.matcher(code.getCode()).matches())
            .map(code -> name(code) + " = " + code.getCode())
            .toList();

        assertThat(invalid).as("code는 의미 있는 대문자 문자열이어야 함 (예: DUPLICATE_NICKNAME). HTTP 상태는 code에 넣지 않는다 (#115)").isEmpty();
    }

    @Test
    void docs_api_spec의_에러_코드_표와_코드가_일치한다() throws Exception {
        Map<String, Integer> documented = new HashMap<>();
        Matcher matcher = DOC_ROW.matcher(Files.readString(API_SPEC));
        while (matcher.find()) {
            documented.put(matcher.group(2), Integer.parseInt(matcher.group(1)));
        }

        List<String> missingInDoc = allCodes.stream()
            .filter(code -> !documented.containsKey(code.getCode()))
            .map(code -> code.getHttpStatus().value() + " " + code.getCode())
            .toList();
        List<String> wrongStatus = allCodes.stream()
            .filter(code -> documented.containsKey(code.getCode()))
            .filter(code -> documented.get(code.getCode()) != code.getHttpStatus().value())
            .map(code -> code.getCode() + ": 코드 " + code.getHttpStatus().value() + ", 문서 " + documented.get(code.getCode()))
            .toList();
        List<String> onlyInDoc = documented.keySet().stream()
            .filter(documentedCode -> allCodes.stream().noneMatch(code -> code.getCode().equals(documentedCode)))
            .sorted()
            .toList();

        // 한 번 실행으로 어긋난 곳을 전부 보여주도록 soft assertion
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(missingInDoc).as("docs/api-spec.md 에러 코드 표에 한 줄 추가할 것").isEmpty();
            softly.assertThat(wrongStatus).as("문서의 HTTP 상태가 코드와 다름").isEmpty();
            softly.assertThat(onlyInDoc).as("문서에만 있고 코드에는 없는 code — 삭제됐거나 이름이 바뀌었으면 문서에서도 고칠 것").isEmpty();
        });
    }

    private static String name(BaseErrorCode code) {
        return code.getClass().getSimpleName() + "." + ((Enum<?>) code).name();
    }
}

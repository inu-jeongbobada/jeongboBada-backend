package com.inu.jeongbobada.domain.course.service;
import com.inu.jeongbobada.domain.course.dto.response.CourseDetailResDto;
import com.inu.jeongbobada.domain.course.dto.response.CourseListResDto;
import com.inu.jeongbobada.domain.course.dto.response.CourseOfferingResDto;
import com.inu.jeongbobada.domain.course.entity.Course;
import com.inu.jeongbobada.domain.course.exception.CourseException;
import com.inu.jeongbobada.domain.course.repository.CourseOfferingRepository;
import com.inu.jeongbobada.domain.course.repository.CourseRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor //생성자 생성
@Transactional(readOnly = true)
// 조회 기능 readOnly = true
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;


    // 과목 리스트 전체 조회 (이번 학기 개설된 강의 기준)
    public List<CourseListResDto> getCourses() {
        return courseOfferingRepository.findAllByActiveTrue()
            .stream()
            .map(CourseListResDto::from)
            .toList();
    }



    // 과목 상세 조회
    public CourseDetailResDto getCourse(Long courseId) {

        Course course = courseRepository.findById(courseId)
            .orElseThrow(()-> new BusinessException(CourseException.COURSE_NOT_FOUND));

        List<CourseOfferingResDto> offerings = courseOfferingRepository
            .findAllByCourse_CourseIdAndActiveTrue(courseId)
            .stream()
            .map(CourseOfferingResDto::from)
            .toList();

        return new CourseDetailResDto(
            course.getCourseId(),
            course.getCourseCode(),
            course.getCourseName(),
            course.getCourseDetail(),
            offerings
        );
    }
}

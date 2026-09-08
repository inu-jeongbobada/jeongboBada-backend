package com.inu.jeongbobada.domain.course.service;
import com.inu.jeongbobada.domain.course.dto.response.CourseDetailResDto;
import com.inu.jeongbobada.domain.course.dto.response.CourseListResDto;
import com.inu.jeongbobada.domain.course.entity.Course;
import com.inu.jeongbobada.domain.course.exception.CourseException;
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


    // 과목 리스트 전체 조회
    public List<CourseListResDto> getCourses() {
        return courseRepository.findAll()
            .stream()
            .map(course -> new CourseListResDto(
                course.getCourseName(),
                course.getProfessorName(),
                course.getGrade(),
                course.getSemester(),
                course.getCredits(),
                course.getCourseCode(),
                course.getCourseType()
            ))
            .toList();
    }



    // 과목 상세 조회
    @Transactional(readOnly = true)
    public CourseDetailResDto getCourse(Long courseId) {

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException(CourseException.COURSE_NOT_FOUND));

        return new CourseDetailResDto(

            course.getCourseName(),
            course.getProfessorName(),
            course.getCourseDetail(),
            course.getGrade(),
            course.getSemester(),
            course.getCredits(),
            course.getCourseCode(),
            course.getCourseTime(),
            course.getCourseType(),
            course.getProfessor(),
            course.getEvaluationType(),
            course.getIsOnline()
        );


    }
}





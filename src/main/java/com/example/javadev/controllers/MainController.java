package com.example.javadev.controllers;

import com.example.javadev.model.Lecture;
import com.example.javadev.model.User;
import com.example.javadev.repository.LectureRepository;
import com.example.javadev.service.Service;
import com.example.javadev.service.ServiceImpl;
import com.example.javadev.repository.UserRepository;
import com.example.javadev.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.Date;

@Controller
@RequestMapping(path = "/home")
public class MainController {

    @Autowired
    private LectureRepository lectureRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Service service;
    @Autowired
    private UserService userService;


    @GetMapping(value = "/mylogin")
    public String getLoginPage(Model model) {
        return "loginpage";
    }

    @GetMapping(value = "/page")
    public String page(Model model, HttpServletRequest request) {
        model.addAttribute("user", request.getRemoteUser());
        return "page";
    }

    @GetMapping(value="/default")
    public String defaultAfterLogin(HttpServletRequest request) {
        if (request.isUserInRole("ADMIN")) {
            return "redirect:/home/attendancelist";
        }
        return "redirect:/home/mylectures";
    }

    @GetMapping(value="/accessdenied")
    public String accessDeniedPage(HttpServletRequest request, Model model) {
        model.addAttribute("user", request.getRemoteUser());
        return "access_denied";
    }

    @GetMapping(value = "/addlectures")
    public String getAddLectures(Model model, HttpServletRequest request) {
        model.addAttribute("user", request.getRemoteUser());
        model.addAttribute("isadiingnewlecturespossible", service.isNumberOfLecturesMoreThan8());
        return "add_lectures";
    }

    @PostMapping(value = "/addlectures")
    public ModelAndView createLecture(
            @RequestParam("lecture_topic") String lecture_topic,
            @RequestParam("lecture_place") String lecture_place,
            @RequestParam("lecture_date") @DateTimeFormat(pattern = "yyyy-MM-dd") Date lecture_date) {
        return new ModelAndView("redirect:/home/addlectures");
    }

    @GetMapping(value = "/mylectures")
    public String getMyLectures(Model model, HttpServletRequest request) {
        model.addAttribute("lectures", lectureRepository.findAll());
        model.addAttribute("user", request.getRemoteUser());
        model.addAttribute("user_id", userRepository.findStudentsIdByEmail(request.getRemoteUser()));
        model.addAttribute("numberOfLecturesAttendedByStudent",
                service.getNumberOfLecturesAttendedByStudent(userRepository.findStudentsIdByEmail(request.getRemoteUser())));
        model.addAttribute("numberOfAllLectures", service.getNumberOfAllLectures());
        return "my_lectures";
    }

    @PostMapping(value = "/lectureattended")
    @Transactional
    public ModelAndView lectureAttended(@RequestParam("user_id") int user_id,
                                        @RequestParam("lecture_id") int lecture_id) {

        User user = userRepository.findByUserId(user_id);
        Lecture lecture = lectureRepository.findOne(lecture_id);

        user.getLectures().add(lecture);
        lecture.getUsers().add(user);

        userRepository.save(user);

        return new ModelAndView("redirect:/home/mylectures");
    }

    @GetMapping(value = "/attendancelist")
    public String getAttendanceList(Model model, HttpServletRequest request) {
        model.addAttribute("lectures", service.createListOfLecturesSortedByDate());
        model.addAttribute("numberoflectures", lectureRepository.count());
        model.addAttribute("user", request.getRemoteUser());
        model.addAttribute("attendancelist", service.createAttendanceList());
        model.addAttribute("listnumberofstudentspresentonlecture", service.createListNumberOfStudentsPresentOnLecture());
        model.addAttribute("numberofstudents", userRepository.findOnlyStudentIds().size());

        return "attendance_list";
    }

    @GetMapping(value = "/studentslist")
    public String getStudentsList(Model model, HttpServletRequest request) {
        model.addAttribute("students", service.createStudentsList());
        model.addAttribute("user", request.getRemoteUser());
        model.addAttribute("numberOfStudents", service.getNumberOfStudents());
        return "students_list";
    }


    @PostMapping(value = "/students/{id}")
    public String deleteStudent(@PathVariable int id) {
        userRepository.deleteUserByUserId(id);
        return "redirect:/home/studentslist";
    }


    @GetMapping(value = "/students/{id}")
    public String getEditStudentPage(@PathVariable int id, Model model, HttpServletRequest request) {
        User student = userRepository.findUserByUserId(id);
        model.addAttribute("student", student);
        model.addAttribute("user", request.getRemoteUser());
        return "edit_user";
    }

    @PostMapping(value = "/students/edit/{id}")
    public String editUser(@PathVariable("id") int id,
                           @RequestParam("firstName") String firstName,
                           @RequestParam("lastName") String lastName,
                           @RequestParam("email") String email) {
        User user = userRepository.findUserByUserId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        userRepository.save(user);
        return "redirect:/home/studentslist";
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logoutPage(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/home/mylogin?logout";
    }

}

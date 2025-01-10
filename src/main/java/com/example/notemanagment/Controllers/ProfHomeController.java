package com.example.notemanagment.Controllers;

import com.example.notemanagment.Models.*;
import com.example.notemanagment.Models.Module;
import com.example.notemanagment.Repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/Dashboard/prof")
public class ProfHomeController {

    @Autowired
    private HttpSession session;

    @Autowired
    private ModuleElementRepository moduleElementRepo;

    @Autowired
    private ProfRepo profRepo;

    @Autowired
    private StudentRepo studentRepo;

    @Autowired
    private EvaluationRepo evaluationRepo ;

    @Autowired
    private NoteRepo noteRepo ;

    @GetMapping("prof")
    public String showProfDashboard(Model model) {
        Integer userId = (Integer) session.getAttribute("userId");

        if (userId == null) {
            System.out.println("Error: User ID is null.");
            return "error"; // Redirect to an error page
        }

        System.out.println("Successfully retrieved the ID: " + userId);

        // Fetch professor details
        Optional<Professor> profOptional = profRepo.findById(userId);
        if (profOptional.isEmpty()) {
            System.out.println("Error: No professor found with ID " + userId);
            return "error"; // Redirect to an error page if the professor is not found
        }

        Professor prof = profOptional.get();

        System.out.println("Professor Name: " + prof.getName());

        // Fetch module elements associated with this professor
        List<ModuleElement> moduleElements = moduleElementRepo.findByProfessorId(userId);

        // Log the number of module elements
        System.out.println("Number of module elements: " + moduleElements.size());

        // Fetch evaluations for each module element
        for (ModuleElement element : moduleElements) {
            List<Evaluation> evaluations = element.getEvaluations();
            element.setEvaluations(evaluations); // Ensure evaluations are populated
        }

        // Add data to the model
        model.addAttribute("userId", userId);
        model.addAttribute("username", prof.getName());
        model.addAttribute("elements", moduleElements);

        return "Dashboard/prof/prof";
    }


    // get to edit the notes of your class:
    @GetMapping("/listEtudiants")
    public String listEtudiants(@RequestParam("elementId") Long elementId, Model model) {
        // Existing logic
        return "Dashboard/prof/listEtudiants";
    }





    @GetMapping("/evaluationDetails")
    public String evaluationDetails(@RequestParam("evaluationId") Long evaluationId, Model model) {
        // Fetch evaluation details
        Optional<Evaluation> evaluationOptional = evaluationRepo.findById(evaluationId);
        if (evaluationOptional.isEmpty()) {
            System.out.println("Error: No evaluation found with ID " + evaluationId);
            return "error"; // Redirect to an error page if evaluation not found
        }

        Evaluation evaluation = evaluationOptional.get();
        ModuleElement moduleElement = evaluation.getModuleElement();

        // Fetch students of the same field and semester as the module element
        List<Student> students = studentRepo.findByFieldAndSemester(
                moduleElement.getModule().getField(),
                moduleElement.getModule().getSemester()
        );

        // Add data to the model
        model.addAttribute("evaluation", evaluation);
        model.addAttribute("students", students);

        return "Dashboard/prof/evaluationDetails";
    }

    @PostMapping("/saveMarks")
    public ResponseEntity<String> saveMarks(@RequestBody List<MarkSubmission> submissions) {
        for (MarkSubmission submission : submissions) {
            // Fetch student and evaluation
            Student student = studentRepo.findById(submission.getStudentId())
                    .orElse(null);
            Evaluation evaluation = evaluationRepo.findById(submission.getEvaluationId())
                    .orElse(null);

            if (student == null || evaluation == null) {
                System.out.println("Error: Missing student or evaluation for submission: " + submission);
                continue; // Skip invalid submissions
            }

            // Check if the Note exists, otherwise create a new one
            Note note = noteRepo.findByStudentAndEvaluation(student, evaluation).orElse(new Note());
            note.setStudent(student);
            note.setEvaluation(evaluation);
            note.setValue(submission.getMark());

            // Save the Note
            noteRepo.save(note);
        }
        return ResponseEntity.ok("Marks saved successfully!");
    }
}
package com.example.notemanagment.Controllers;

import com.example.notemanagment.Models.*;
import com.example.notemanagment.Repository.EvaluationRepo;
import com.example.notemanagment.Repository.NoteRepo;
import com.example.notemanagment.Repository.StudentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/Dashboard/prof")
public class evaluationDetails_Controller {
    @Autowired
    private StudentRepo studentRepo;

    @Autowired
    private EvaluationRepo evaluationRepo ;

    @Autowired
    private NoteRepo noteRepo ;
    @GetMapping("/evaluationDetails")
    public String evaluationDetails(@RequestParam("evaluationId") Long evaluationId, Model model) {
        Optional<Evaluation> evaluationOptional = evaluationRepo.findById(evaluationId);
        if (evaluationOptional.isEmpty()) {
            System.out.println("Error: No evaluation found with ID " + evaluationId);
            return "error";
        }

        Evaluation evaluation = evaluationOptional.get();
        ModuleElement moduleElement = evaluation.getModuleElement();

        List<Student> students = studentRepo.findByFieldAndSemester(
                moduleElement.getModule().getField(),
                moduleElement.getModule().getSemester()
        );

        // Prepare student notes
        List<Map<String, Object>> studentNotes = students.stream().map(student -> {
            Note note = noteRepo.findByStudentAndEvaluation(student, evaluation).orElse(null);
            Map<String, Object> studentData = new HashMap<>();
            studentData.put("id", student.getId());
            studentData.put("name", student.getName());
            studentData.put("surname", student.getSurname());
            studentData.put("note", note != null ? note.getValue() : 0);
            return studentData;
        }).toList();

        model.addAttribute("evaluation", evaluation);
        model.addAttribute("students", studentNotes);

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
    @PostMapping("/validateEvaluation")
    public ResponseEntity<String> validateEvaluation(@RequestParam("evaluationId") Long evaluationId) {
        Optional<Evaluation> evaluationOptional = evaluationRepo.findById(evaluationId);
        if (evaluationOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Evaluation not found.");
        }

        Evaluation evaluation = evaluationOptional.get();
        evaluation.setIsValidated(true);
        evaluationRepo.save(evaluation);

        return ResponseEntity.ok("Evaluation validated.");
    }
}

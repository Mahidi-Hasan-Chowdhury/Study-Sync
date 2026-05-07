package bd.edu.seu.studysync.controller;

import bd.edu.seu.studysync.model.Quiz;
import bd.edu.seu.studysync.model.QuizRoom;
import bd.edu.seu.studysync.model.RoomParticipant;
import bd.edu.seu.studysync.model.User;
import bd.edu.seu.studysync.service.QuizAiService;
import bd.edu.seu.studysync.service.QuizRoomService;
import bd.edu.seu.studysync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/room")
@RequiredArgsConstructor
public class QuizRoomController {

    private final QuizRoomService quizRoomService;
    private final UserService userService;
    private final QuizAiService quizAiService;

    @GetMapping
    public String roomDashboard(Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) return "redirect:/login";

        List<QuizRoom> myRooms = quizRoomService.getMyRooms(currentUser.get().getId());
        List<Quiz> myQuizzes = quizAiService.getQuizzesByUserId(currentUser.get().getId());

        model.addAttribute("rooms", myRooms);
        model.addAttribute("quizzes", myQuizzes);
        model.addAttribute("contentPage", "room-dashboard");
        return "layout";
    }

    @PostMapping("/create")
    public String createRoom(@RequestParam String quizId,
                             @RequestParam String title,
                             @RequestParam int durationHours,
                             RedirectAttributes redirectAttributes) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) return "redirect:/login";

        try {
            LocalDateTime deadline = LocalDateTime.now().plusHours(durationHours);
            QuizRoom room = quizRoomService.createRoom(currentUser.get().getId(), quizId, title, deadline);
            redirectAttributes.addFlashAttribute("success", "Room created successfully!");
            return "redirect:/room/" + room.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating room: " + e.getMessage());
            return "redirect:/room";
        }
    }

    @PostMapping("/join")
    public String joinRoom(@RequestParam String accessCode, RedirectAttributes redirectAttributes) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) return "redirect:/login";

        try {
            RoomParticipant participant = quizRoomService.joinRoomByCode(accessCode.trim().toUpperCase(), currentUser.get().getId());
            return "redirect:/room/" + participant.getRoomId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/room";
        }
    }

    @GetMapping("/{id}")
    public String roomDetails(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) return "redirect:/login";

        Optional<QuizRoom> roomOpt = quizRoomService.getRoomById(id);
        if (roomOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Room not found");
            return "redirect:/room";
        }
        QuizRoom room = roomOpt.get();

        List<RoomParticipant> leaderboard = quizRoomService.getLeaderboard(id);
        
        Optional<RoomParticipant> myParticipation = leaderboard.stream()
                .filter(p -> p.getUserId().equals(currentUser.get().getId()))
                .findFirst();

        if (myParticipation.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "You must join this room first.");
            return "redirect:/room";
        }

        model.addAttribute("room", room);
        model.addAttribute("leaderboard", leaderboard);
        model.addAttribute("myStatus", myParticipation.get());
        

        
        model.addAttribute("contentPage", "room-details");
        return "layout";
    }
}

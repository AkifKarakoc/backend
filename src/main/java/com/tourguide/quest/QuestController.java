package com.tourguide.quest;

import com.tourguide.quest.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    @GetMapping
    public ResponseEntity<List<QuestResponse>> getAllQuests(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(questService.getAllQuests(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestDetailResponse> getQuestDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(questService.getQuestDetail(id, userId));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<StartQuestResponse> startQuest(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questService.startQuest(userId, id));
    }

    @PostMapping(value = "/{id}/steps/{stepId}/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VerifyStepResponse> verifyStep(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @PathVariable UUID stepId,
            @RequestParam(required = false) MultipartFile photo,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        return ResponseEntity.ok(questService.verifyStep(userId, id, stepId, photo, latitude, longitude));
    }

    @GetMapping("/user/quests")
    public ResponseEntity<List<UserQuestResponse>> getUserQuests(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(questService.getUserQuests(userId));
    }
}

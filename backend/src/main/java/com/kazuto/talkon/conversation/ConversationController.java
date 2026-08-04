package com.kazuto.talkon.conversation;
import com.kazuto.talkon.auth.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/conversations")
public class ConversationController {
 private final ConversationService service; public ConversationController(ConversationService s){service=s;}
 public record MessageRequest(@NotBlank @Size(max=2000) String content){}
 @PostMapping public ResponseEntity<ConversationDtos.Detail> start(Authentication a){var result=service.start(CurrentUser.require(a).id());return ResponseEntity.status(result.created()?201:200).body(result.detail());}
 @GetMapping("/active") public ResponseEntity<ConversationDtos.Detail> active(Authentication a){var d=service.active(CurrentUser.require(a).id());return d==null?ResponseEntity.noContent().build():ResponseEntity.ok(d);}
 @GetMapping("/{id}") public ConversationDtos.Detail detail(@PathVariable Long id,Authentication a){return service.detail(id,CurrentUser.require(a).id());}
 @PostMapping("/{id}/messages") public ConversationDtos.Detail send(@PathVariable Long id,@Valid @RequestBody MessageRequest r,Authentication a){return service.send(id,CurrentUser.require(a).id(),r.content());}
 @PostMapping("/{id}/finish") public ConversationDtos.Detail finish(@PathVariable Long id,Authentication a){return service.finish(id,CurrentUser.require(a).id());}
 @GetMapping public ConversationDtos.PageResponse history(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,Authentication a){return service.history(CurrentUser.require(a).id(),page,size);}
}


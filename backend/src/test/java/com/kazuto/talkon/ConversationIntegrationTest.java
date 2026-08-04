package com.kazuto.talkon;
import com.kazuto.talkon.auth.TalkOnPrincipal;
import com.kazuto.talkon.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class ConversationIntegrationTest {
 @Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired PasswordEncoder encoder;User owner;User other;
 @BeforeEach void setup(){users.deleteAll();owner=users.save(new User("Owner","owner@example.com",encoder.encode("password123")));other=users.save(new User("Other","other@example.com",encoder.encode("password123")));}
 private org.springframework.test.web.servlet.request.RequestPostProcessor auth(User u){var p=new TalkOnPrincipal(u.getId(),u.getEmail(),u.getDisplayName());return authentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(p,null,java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));}
 @Test void conversationIsOrderedAndOwnershipIsHidden() throws Exception {
  String json=mvc.perform(post("/api/conversations").with(auth(owner)).with(csrf())).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();long id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).path("id").asLong();
  mvc.perform(post("/api/conversations/"+id+"/messages").with(auth(owner)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"I like hiking.\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.messages[1].role").value("USER")).andExpect(jsonPath("$.messages[2].role").value("ASSISTANT"));
  mvc.perform(get("/api/conversations/"+id).with(auth(other))).andExpect(status().isNotFound());
  mvc.perform(post("/api/conversations/"+id+"/finish").with(auth(owner)).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED")).andExpect(jsonPath("$.feedback.summary").exists());
  mvc.perform(post("/api/conversations/"+id+"/messages").with(auth(owner)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"again\"}")).andExpect(status().isConflict());
 }
}

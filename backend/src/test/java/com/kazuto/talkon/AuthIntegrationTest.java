package com.kazuto.talkon;
import com.kazuto.talkon.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@SpringBootTest @AutoConfigureMockMvc
class AuthIntegrationTest {
 @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired PasswordEncoder encoder;
 @Test void registrationHashesPasswordAndRejectsDuplicate() throws Exception {
  String body="{\"displayName\":\" Test User \",\"email\":\"TEST@example.com\",\"password\":\"password123\"}";
  mvc.perform(post("/api/auth/register").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
  var saved=users.findByEmail("test@example.com").orElseThrow();assertThat(saved.getPasswordHash()).isNotEqualTo("password123");assertThat(encoder.matches("password123",saved.getPasswordHash())).isTrue();
  mvc.perform(post("/api/auth/register").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
 }
 @Test void protectedApiReturns401(){try{mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());}catch(Exception e){throw new RuntimeException(e);}}
 @Test void loginCreatesSessionAndBadPasswordIsRejected() throws Exception {
  users.save(new com.kazuto.talkon.user.User("Login User","login@example.com",encoder.encode("password123")));
  var login=mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"login@example.com\",\"password\":\"password123\"}")).andExpect(status().isOk()).andReturn();
  mvc.perform(get("/api/auth/me").session((org.springframework.mock.web.MockHttpSession)login.getRequest().getSession(false))).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("login@example.com"));
  mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"login@example.com\",\"password\":\"wrong-password\"}")).andExpect(status().isUnauthorized());
 }
}

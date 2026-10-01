package com.talkon.speechservice.speech;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.talkon.speechservice.security.InternalKeyInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.internal-key=test-key")
@AutoConfigureMockMvc
class SpeechControllerTest {
  @Autowired MockMvc mvc;

  @Test
  void rejectsRequestsWithoutInternalKey() throws Exception {
    mvc.perform(
            multipart("/internal/speech/transcriptions")
                .file(new MockMultipartFile("audio", "sample.webm", "audio/webm", new byte[32])))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void transcribesWithLocalProvider() throws Exception {
    mvc.perform(
            multipart("/internal/speech/transcriptions")
                .file(new MockMultipartFile("audio", "sample.webm", "audio/webm", new byte[32]))
                .header(InternalKeyInterceptor.HEADER, "test-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.text").value("I like practicing English with TalkOn."))
        .andExpect(jsonPath("$.model").value("local-stt"));
  }

  @Test
  void synthesizesWithLocalProvider() throws Exception {
    mvc.perform(
            post("/internal/speech/synthesis")
                .header(InternalKeyInterceptor.HEADER, "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Hello\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Speech-Model", "local-tts"))
        .andExpect(content().contentType("audio/wav"));
  }
}

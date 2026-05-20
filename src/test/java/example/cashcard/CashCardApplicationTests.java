package example.cashcard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import org.springframework.test.annotation.DirtiesContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hamcrest.Matchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashCardApplicationTests {
    @Autowired
    MockMvc mockMvc;

    private static RequestPostProcessor sarahAuth() {
        return httpBasic("sarah1", "abc123");
    }

    @Test
    void shouldReturnACashCardWhenDataIsSaved() throws Exception {
        mockMvc.perform(
                get("/cashcards/99").with(sarahAuth()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(99),
                        jsonPath("$.amount").value(123.45));
    }

    @Test
    void shouldNotReturnACashCardWithAnUnknownId() throws Exception {
        mockMvc.perform(get("/cashcards/1000").with(sarahAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DirtiesContext
    void shouldCreateANewCashCard() throws Exception {
        var location = mockMvc.perform(
                post("/cashcards")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .with(sarahAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 250.0}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("location"))
                .andReturn()
                .getResponse()
                .getHeader("location");

        mockMvc.perform(
                get(location)
                        .with(sarahAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(250));
    }

    @Test
    void shouldReturnAllCashCardsWhenListIsRequired() throws Exception {
        mockMvc.perform(get("/cashcards").with(sarahAuth()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(3),
                        jsonPath("$[*].id").value(Matchers.containsInAnyOrder(99, 100, 101)));
    }

    @Test
    void shouldReturnAPageOfCashCards() throws Exception {
        mockMvc.perform(get("/cashcards?page=0&size=1").with(sarahAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldReturnADescSortedPageOfCashCards() throws Exception {
        mockMvc
                .perform(get("/cashcards?page=0&size=1&sort=amount,desc").with(sarahAuth()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1),
                        jsonPath("$[0].amount").value(150));
    }

    @Test
    void shouldReturnAAscSortedPageOfCashCards() throws Exception {
        mockMvc
                .perform(get("/cashcards?page=0&size=1&sort=amount,asc").with(sarahAuth()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1),
                        jsonPath("$[0].amount").value(1));
    }

    @Test
    void shouldReturnASortedPageOfCashCardWithNoParameters() throws Exception {
        mockMvc
                .perform(get("/cashcards").with(sarahAuth()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(3),
                        jsonPath("$[0].amount").value(1));
    }

    @Test
    void shouldRejectUsersWhoAreNotCardOwners() throws Exception {
        mockMvc.perform(get("/cashcards/99").with(httpBasic("hank", "abc123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateAnExistingCashCard() throws Exception {
        mockMvc.perform(
                put("/cashcards/99")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .with(sarahAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":19.99}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                get("/cashcards/99")
                        .with(sarahAuth()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(99),
                        jsonPath("$.amount").value(19.99));
    }

    @Test
    void shouldNotUpdateACashCardThatDoesNotExist() throws Exception {
        mockMvc.perform(
                put("/cashcards/99999")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .with(sarahAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":19.99}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotUpdateACashCardThatIsOwnedBySomeoneElse() throws Exception {
        mockMvc.perform(
                put("/cashcards/102")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .with(sarahAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":19.99}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DirtiesContext
    void shouldDeleteAnExistingCashCard() throws Exception {
        mockMvc.perform(
                delete("/cashcards/99")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .with(sarahAuth()))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                get("/cashcards/99")
                        .with(sarahAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shoudNotDeleteACashCardThatDoesNotExist() throws Exception {
        mockMvc.perform(
                delete("/cashcards/99999")
                        .with(csrf())
                        .with(sarahAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotAllowDeletionOfCashCardsTheyDoNotOwn() throws Exception {
        mockMvc.perform(
                delete("/cashcards/102")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .with(sarahAuth()))
                .andExpect(status().isNotFound());

        mockMvc.perform(
                delete("/cashcards/102")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .with(httpBasic("kumar2", "abc123")))
                .andExpect(status().isNoContent());
    }
}
package example.cashcard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashCardApplicationTests {
    @Autowired
    WebTestClient restClient;

    @Test
    void shouldReturnACashCardWhenDataIsSaved() {
        var cashCard = restClient.get()
                .uri("/cashcards/99")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CashCard.class)
                .returnResult()
                .getResponseBody();
        assertThat(cashCard.id()).isEqualTo(99);
        assertThat(cashCard.amount()).isEqualTo(123.45);
    }

    @Test
    void shouldNotReturnACashCardWithAnUnknownId() {
        restClient.get().uri("/cashcards/1000").exchange().expectStatus().isNotFound();
    }

    @Test
    @DirtiesContext
    void shouldCreateANewCashCard() {
        var newCashCard = new CashCard(null, 250.00);
        var location = restClient.post()
                .uri("/cashcards")
                .bodyValue(newCashCard)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("location")
                .returnResult()
                .getResponseHeaders()
                .getLocation();

        var savedCashCard = restClient.get()
                .uri(location)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CashCard.class)
                .returnResult()
                .getResponseBody();

        assertThat(savedCashCard).isNotNull();
        assertThat(savedCashCard.id()).isNotNull();
        assertThat(savedCashCard.amount()).isEqualTo(250.00);
    }

    @Test
    void shouldReturnAllCashCardsWhenListIsRequired() {
        var cards = restClient.get()
                .uri("/cashcards").exchange()
                .expectStatus().isOk()
                .expectBodyList(CashCard.class)
                .returnResult().getResponseBody();

        assertThat(cards).hasSize(3);
        assertThat(cards).extracting(CashCard::id).containsExactlyInAnyOrder(99L, 100L, 101L);
    }

    @Test
    void shouldReturnAPageOfCashCards() {
        var cards = restClient.get()
                .uri("/cashcards?page=0&size=1")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CashCard.class)
                .returnResult()
                .getResponseBody();
        assertThat(cards).hasSize(1);
    }

    @Test
    void shouldReturnADescSortedPageOfCashCards() {
        var cards = restClient.get()
                .uri("/cashcards?page=0&size=1&sort=amount,desc")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CashCard.class)
                .returnResult()
                .getResponseBody();
        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).amount()).isEqualTo(150.0);
    }

    @Test
    void shouldReturnAAscSortedPageOfCashCards() {
        var cards = restClient.get()
                .uri("/cashcards?page=0&size=1&sort=amount,asc")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CashCard.class)
                .returnResult()
                .getResponseBody();
        assertThat(cards).hasSize(1);
        assertThat(cards.get(0)).hasFieldOrPropertyWithValue("amount", 1.0);
    }

    @Test
    void shouldReturnASortedPageOfCashCardWithNoParameters() {
        var cards = restClient.get()
                .uri("/cashcards")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CashCard.class)
                .returnResult()
                .getResponseBody();
        assertThat(cards).hasSize(3);
        assertThat(cards.get(0)).hasFieldOrPropertyWithValue("amount", 1.0);
    }
}
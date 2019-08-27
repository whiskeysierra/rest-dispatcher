package org.zalando.riptide;

import com.google.common.collect.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.client.*;

import static org.springframework.http.HttpStatus.Series.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;

final class RequesterTest {

    private final Http unit;
    private final MockRestServiceServer server;

    RequesterTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
        this.server = setup.getServer();
    }

    @AfterEach
    void after() {
        server.verify();
    }

    @Test
    void shouldExpandWithoutVariables() {
        expectRequestTo("https://api.example.com/123");

        unit.get("/123")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

    @Test
    void shouldExpandOne() {
        expectRequestTo("https://api.example.com/123");

        unit.get("/{id}", 123)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

    @Test
    void shouldExpandTwo() {
        expectRequestTo("https://api.example.com/123/456");

        unit.get("/{parent}/{child}", 123, "456")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

    @Test
    void shouldExpandInlinedQueryParams() {
        expectRequestTo("https://example.com/posts/123?filter=new");

        final int postId = 123;
        final String filter = "new";

        unit.get("https://example.com/posts/{id}?filter={filter}", postId, filter)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

    @Test
    void shouldEncodePath() {
        expectRequestTo(
                "https://ru.wikipedia.org/wiki/%D0%9E%D1%82%D0%B1%D0%BE%D0%B9%D0%BD%D0%BE%D0%B5_%D1%82%D0%B5%D1%87%D0%B5%D0%BD%D0%B8%D0%B5");

        unit.get("https://ru.wikipedia.org/wiki/{article-name}", "Отбойное_течение")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

    @Test
    void shouldEncodeInlinedQueryParams() {
        expectRequestTo(
                "https://ru.wiktionary.org/w/index.php?title=%D0%A1%D0%BB%D1%83%D0%B6%D0%B5%D0%B1%D0%BD%D0%B0%D1%8F:%D0%9A%D0%BE%D0%BB%D0%BB%D0%B5%D0%BA%D1%86%D0%B8%D1%8F_%D0%BA%D0%BD%D0%B8%D0%B3&bookcmd=book_creator&referer=%D0%97%D0%B0%D0%B3%D0%BB%D0%B0%D0%B2%D0%BD%D0%B0%D1%8F%20%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B8%D1%86%D0%B0");

        unit.get("https://ru.wiktionary.org/w/index.php?title={title}&bookcmd=book_creator&referer={referer}",
                "Служебная:Коллекция_книг", "Заглавная страница")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

    @Test
    void shouldAppendQueryParams() {
        server.expect(requestTo("https://api.example.com?foo=bar&foo=baz&bar=null"))
                .andRespond(withSuccess());

        unit.head("https://api.example.com")
                .queryParam("foo", "bar")
                .queryParams(ImmutableMultimap.of(
                        "foo", "baz",
                        "bar", "null"
                ))
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

    @Test
    void shouldEncodeAppendedQueryParams() {
        expectRequestTo(
                "https://ru.wiktionary.org/w/index.php?bookcmd=book_creator&referer=%D0%97%D0%B0%D0%B3%D0%BB%D0%B0%D0%B2%D0%BD%D0%B0%D1%8F%20%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B8%D1%86%D0%B0&title=%D0%A1%D0%BB%D1%83%D0%B6%D0%B5%D0%B1%D0%BD%D0%B0%D1%8F%3A%D0%9A%D0%BE%D0%BB%D0%BB%D0%B5%D0%BA%D1%86%D0%B8%D1%8F_%D0%BA%D0%BD%D0%B8%D0%B3");

        unit.head("https://ru.wiktionary.org/w/index.php")
                .queryParam("title", "Служебная:Коллекция_книг")
                .queryParam("bookcmd", "book_creator")
                .queryParam("referer", "Заглавная страница")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

    @Test
    void shouldAppendedDateTimeQueryParams() {
        expectRequestTo(
                "https://test.datetimes.org/index.php?to=2018-05-21T10%3A24%3A47.788%2B00%3A00&from=2016-04-20T09%3A23%3A46.787Z");

        unit.head("https://test.datetimes.org/index.php")
                .queryParam("to", "2018-05-21T10:24:47.788+00:00")
                .queryParam("from", "2016-04-20T09:23:46.787Z")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

    @Test
    void shouldAppendMultiValueQueryParams() {
        expectRequestTo("https://test.datetimes.org/index.php?team_id=1&team_id=2");

        unit.head("https://test.datetimes.org/index.php")
                .queryParam("team_id", "1")
                .queryParam("team_id", "2")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

    @Test
    void shouldExpandOnGetWithHeaders() {
        expectRequestTo("https://api.example.com/123");

        unit.get("/123")
                .headers(ImmutableMultimap.of())
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

    @Test
    void shouldExpandOnGetWithBody() {
        expectRequestTo("https://api.example.com/123");

        unit.get("/123")
                .body("deadbody")
                .call(pass())
                .join();
    }

    @Test
    void shouldExpandOnGetWithHeadersAndBody() {
        expectRequestTo("https://api.example.com/123");

        unit.get("/123")
                .headers(ImmutableMultimap.of())
                .body("deadbody")
                .call(pass())
                .join();
    }

    private void expectRequestTo(final String url) {
        server.expect(requestTo(url))
                .andRespond(withSuccess());
    }

}

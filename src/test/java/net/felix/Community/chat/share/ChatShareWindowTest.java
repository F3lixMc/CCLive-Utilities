package net.felix.Community.chat.share;

import java.util.List;
import net.felix.Community.chat.share.ChatShareCatalog.Bind;
import net.felix.Community.chat.share.ChatShareInbox.Hit;
import net.felix.Community.chat.share.ChatShareInbox.Line;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatShareWindowTest {
    private static final ChatShareClock.Sample EXACT = new ChatShareClock.Sample(0, 0, 0);

    @Test
    void offsetUsesTheMidpointAndKeepsTheTighterSample() {
        ChatShareClock clock = new ChatShareClock();
        clock.observe(1_000L, 1_400L, 5_000L);
        ChatShareClock.Sample sample = clock.usable(1_400L);
        assertEquals(3_800L, sample.offsetMs());
        assertEquals(200L, sample.uncertaintyMs());

        clock.observe(2_000L, 2_800L, 6_000L);
        assertEquals(200L, clock.usable(2_800L).uncertaintyMs());

        clock.observe(10_000L, 10_100L, 14_000L);
        assertEquals(50L, clock.usable(10_100L).uncertaintyMs());
        assertEquals(3_950L, clock.usable(10_100L).offsetMs());
    }

    @Test
    void staleOrWideSamplesAreNotUsable() {
        ChatShareClock clock = new ChatShareClock();
        clock.observe(0L, 2_500L, 1_000L);
        assertNull(clock.usable(2_500L));

        ChatShareClock fresh = new ChatShareClock();
        fresh.observe(0L, 100L, 1_000L);
        assertNull(fresh.usable(ChatShareClock.SAMPLE_TTL_MS + 101L));
    }

    @Test
    void windowCoversPlayLegendLatencyAndPrepareSkew() {
        long accepted = 10_000L;
        assertTrue(ChatShareClock.inWindow(accepted, accepted, EXACT));
        assertTrue(ChatShareClock.inWindow(accepted, accepted + ChatShareClock.WINDOW_MS, EXACT));
        assertTrue(!ChatShareClock.inWindow(accepted, accepted + ChatShareClock.WINDOW_MS + 1, EXACT));
        assertTrue(ChatShareClock.inWindow(accepted, accepted - ChatShareClock.EARLY_MS, EXACT));
        assertTrue(!ChatShareClock.inWindow(accepted, accepted - ChatShareClock.EARLY_MS - 1, EXACT));

        ChatShareClock.Sample loose = new ChatShareClock.Sample(0, 500L, accepted);
        assertTrue(ChatShareClock.inWindow(accepted, accepted + ChatShareClock.WINDOW_MS + 500L, loose));
        assertTrue(!ChatShareClock.windowClosed(accepted, accepted + ChatShareClock.WINDOW_MS, EXACT));
        assertTrue(ChatShareClock.windowClosed(accepted, accepted + ChatShareClock.WINDOW_MS + 1, EXACT));
    }

    @Test
    void firstPairLinksImmediatelyAndRetryCannotReuseIt() {
        long received = 20_000L;
        ChatShareInbox inbox = new ChatShareInbox();
        Line first = line("Ada", "test Coin Kit", received);
        Line second = line("Ada", "test Coin Kit", received + 100);
        inbox.accept(first);
        inbox.accept(second);
        Bind bind = bind("Ada", "test Coin Kit", received, 5);
        List<Hit> linked = inbox.applyBundle(List.of(bind), received + 110, EXACT);
        assertEquals(1, linked.size());
        assertEquals(first, linked.getFirst().line());
        assertEquals(bind.attemptId(), linked.getFirst().bind().attemptId());
        assertTrue(inbox.applyBundle(List.of(bind), received + 120, EXACT).isEmpty());
        assertEquals(1, inbox.waiting(received + 120));
    }

    @Test
    void bindingArrivingBeforeChatStillLinksTheFirstLine() {
        long received = 20_000L;
        ChatShareInbox inbox = new ChatShareInbox();
        Bind bind = bind("Ada", "test Coin Kit", received, 5);
        assertTrue(inbox.applyBundle(List.of(bind), received, EXACT).isEmpty());
        Line first = line("Ada", "test Coin Kit", received + 100);
        inbox.accept(first);
        assertEquals(List.of(first), inbox.applyBundle(List.of(), received + 110, EXACT)
                .stream().map(Hit::line).toList());
        inbox.accept(line("Ada", "test Coin Kit", received + 200));
        assertTrue(inbox.applyBundle(List.of(), received + 210, EXACT).isEmpty());
    }

    @Test
    void missingClockDoesNotLink() {
        long received = 20_000L;
        ChatShareInbox inbox = new ChatShareInbox();
        inbox.accept(line("Ada", "test Coin Kit", received));
        assertTrue(inbox.applyBundle(
                List.of(bind("Ada", "test Coin Kit", received, 5)),
                received + ChatShareClock.WINDOW_MS + 5,
                null).isEmpty());
        assertEquals(1, inbox.waiting(received + ChatShareClock.WINDOW_MS + 5));
    }

    @Test
    void eachSeparateSendCanLinkOnceAndOutsideWindowStaysPlain() {
        long firstAt = 30_000L;
        long secondAt = firstAt + ChatShareClock.WINDOW_MS + ChatShareClock.EARLY_MS + 1;
        ChatShareInbox inbox = new ChatShareInbox();
        Line early = line("Ada", "test Coin Kit", firstAt);
        Line late = line("Ada", "test Coin Kit", secondAt);
        inbox.accept(early);
        inbox.accept(late);
        List<Hit> linked = inbox.applyBundle(List.of(
                bind("Ada", "test Coin Kit", firstAt, 5),
                bind("Ada", "test Coin Kit", secondAt, 5)), secondAt + 1, EXACT);
        assertEquals(List.of(early, late), linked.stream().map(Hit::line).toList());

        ChatShareInbox outside = new ChatShareInbox();
        outside.accept(line("Ada", "test Coin Kit", firstAt + ChatShareClock.WINDOW_MS + 1));
        assertTrue(outside.applyBundle(List.of(bind("Ada", "test Coin Kit", firstAt, 5)),
                firstAt + ChatShareClock.WINDOW_MS + 2, EXACT).isEmpty());
    }

    @Test
    void lineStaysUntilTheFirstPollCanLinkIt() {
        long received = 50_000L;
        ChatShareInbox inbox = new ChatShareInbox();
        Line line = line("Ada", "test Coin Kit", received);
        inbox.accept(line);
        long late = received + ChatShareRing.HOLD_MS + 3_000L;
        assertEquals(1, inbox.waiting(late, 0L));
        List<Hit> linked = inbox.applyBundle(
                List.of(bind("Ada", "test Coin Kit", received, 5)),
                late,
                EXACT,
                0L);
        assertEquals(1, linked.size());
        assertEquals(line, linked.get(0).line());
        assertEquals(0, inbox.waiting(late, late));
    }

    @Test
    void echoBeforePrepareConfirmsOnce() {
        ChatSharePublish publish = new ChatSharePublish();
        ChatShareAttempts.Attempt attempt = new ChatShareAttempts().addShare(
                "test Coin Kit", "ITEM", "ITEM-abc123", "Coin Kit", 5, 8, 1_000L);
        assertEquals(ChatSharePublish.Step.PREPARE, publish.noted(attempt).step());
        assertNull(publish.echo(attempt));
        ChatSharePublish.Next confirm = publish.preparedOk(attempt.attemptId());
        assertEquals(ChatSharePublish.Step.CONFIRM, confirm.step());
        assertNull(publish.preparedOk(attempt.attemptId()));
        assertNull(publish.echo(attempt));
    }

    @Test
    void rejectedPrepareAndCancelledSendDoNotConfirm() {
        ChatSharePublish rejected = new ChatSharePublish();
        ChatShareAttempts.Attempt attempt = new ChatShareAttempts().addShare(
                "test Coin Kit", "ITEM", "ITEM-abc123", "Coin Kit", 5, 8, 1_000L);
        rejected.noted(attempt);
        rejected.preparedRejected(attempt.attemptId());
        assertNull(rejected.echo(attempt));

        ChatSharePublish cancelled = new ChatSharePublish();
        cancelled.noted(attempt);
        cancelled.cancel(attempt);
        assertNull(cancelled.preparedOk(attempt.attemptId()));
        assertNull(cancelled.echo(attempt));
    }

    private static ChatShareInbox inboxOf(Line... lines) {
        ChatShareInbox inbox = new ChatShareInbox();
        for (Line line : lines) {
            inbox.accept(line);
        }
        return inbox;
    }

    private static int attemptSeq;

    private static Line line(String sender, String body, long receivedAt) {
        return new Line(sender, body, receivedAt);
    }

    private static Bind bind(String player, String body, long acceptedAt, int start) {
        String name = "Coin Kit";
        return new Bind(++attemptSeq, "attempt-" + attemptSeq, player, ChatShareTexts.sha256(body),
                name, acceptedAt, "ITEM", "ITEM-abc123", start, name.length(), null);
    }
}

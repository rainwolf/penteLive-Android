package be.submanifold.pentelive.blocked;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class BlockFlowTest {

    @Test
    public void messageDialogBlockMessagesOnlyBlocksChat() {
        assertArrayEquals(new boolean[]{false, true}, BlockFlow.flags(false, false));
    }

    @Test
    public void messageDialogBlockBothBlocksInviteAndChat() {
        assertArrayEquals(new boolean[]{true, true}, BlockFlow.flags(false, true));
    }

    @Test
    public void invitationDialogBlockInvitationsOnlyBlocksInvite() {
        assertArrayEquals(new boolean[]{true, false}, BlockFlow.flags(true, false));
    }

    @Test
    public void invitationDialogBlockBothBlocksInviteAndChat() {
        assertArrayEquals(new boolean[]{true, true}, BlockFlow.flags(true, true));
    }
}

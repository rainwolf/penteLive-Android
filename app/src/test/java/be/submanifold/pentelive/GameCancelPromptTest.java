package be.submanifold.pentelive;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GameCancelPromptTest {
    @Test public void cancelPrompt_parentOfPlayer1_noPrompt() {
        assertFalse(Game.isCancelPromptFor("graviton", "kidstb1", "graviton", "iostest"));
        assertFalse(Game.isCancelPromptFor("kidstb1", "kidstb1", "graviton", "iostest"));
    }

    @Test public void cancelPrompt_playerSeesOpponentRequest() {
        assertTrue(Game.isCancelPromptFor("graviton", "kidstb1", "graviton", "kidstb1"));
        assertTrue(Game.isCancelPromptFor("kidstb1", "kidstb1", "graviton", "graviton"));
        // viewer-name match ignores case (:761/:775); cancel name matched exactly, today
        assertTrue(Game.isCancelPromptFor("graviton", "kidstb1", "graviton", "KidsTB1"));
    }

    @Test public void cancelPrompt_ownRequest_noPrompt() {
        assertFalse(Game.isCancelPromptFor("kidstb1", "kidstb1", "graviton", "kidstb1"));
    }

    @Test public void cancelPrompt_noRequest_noPrompt() {
        assertFalse(Game.isCancelPromptFor(null, "kidstb1", "graviton", "kidstb1"));
    }
}

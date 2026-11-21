package net.felix.utilities.Overall.fragmentdrop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FragmentDropAnimationUtilityTest {

	@Test
	void matchesServerFragmentMessage() {
		assertTrue(FragmentDropAnimationUtility.isFragmentDropMessage(
				"§r[Legend] Du hast Schmiedekammer-Talismanfragment gefunden."));
		assertTrue(FragmentDropAnimationUtility.isFragmentDropMessage(
				"[Legend] Du hast Schmiedekammer-Talismanfragment gefunden."));
	}

	@Test
	void rejectsPlayerChatAndOtherMessages() {
		assertFalse(FragmentDropAnimationUtility.isFragmentDropMessage(
				"Spieler >> [Legend] Du hast Schmiedekammer-Talismanfragment gefunden."));
		assertFalse(FragmentDropAnimationUtility.isFragmentDropMessage(
				"Du hast Schmiedekammer-Talismanfragment gefunden."));
		assertFalse(FragmentDropAnimationUtility.isFragmentDropMessage(
				"[Legend] Schmiedezustand erhalten: [Sternengeschmiedet]"));
		assertFalse(FragmentDropAnimationUtility.isFragmentDropMessage(null));
	}
}

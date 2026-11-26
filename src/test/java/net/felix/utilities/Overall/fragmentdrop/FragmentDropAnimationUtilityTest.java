package net.felix.utilities.Overall.fragmentdrop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FragmentDropAnimationUtilityTest {

	@Test
	void extractsNameFromServerFragmentMessage() {
		assertEquals("Schmiedekammer-Talismanfragment", FragmentDropAnimationUtility.extractFragmentName(
				"§r[Legend] Du hast Schmiedekammer-Talismanfragment gefunden."));
		assertEquals("Schmiedekammer-Talismanfragment", FragmentDropAnimationUtility.extractFragmentName(
				"[Legend] Du hast Schmiedekammer-Talismanfragment gefunden."));
	}

	@Test
	void extractsNameFromCurrentFragmentMessage() {
		assertEquals("Anhänger der Schmiedekammer [Fragment]", FragmentDropAnimationUtility.extractFragmentName(
				"[20:21] [Legend] Du hast Anhänger der Schmiedekammer [Fragment] gefunden."));
		assertEquals("Anhänger des Glühenden Hammers [Fragment]", FragmentDropAnimationUtility.extractFragmentName(
				"§r[Legend] Du hast §dAnhänger des Glühenden Hammers §7[Fragment] §rgefunden."));
		assertNull(FragmentDropAnimationUtility.extractFragmentName(
				"Spieler >> [Legend] Du hast Anhänger der Schmiedekammer [Fragment] gefunden."));
	}

	@Test
	void rejectsPlayerChatAndOtherMessages() {
		assertNull(FragmentDropAnimationUtility.extractFragmentName(
				"Spieler >> [Legend] Du hast Schmiedekammer-Talismanfragment gefunden."));
		assertNull(FragmentDropAnimationUtility.extractFragmentName(
				"Du hast Schmiedekammer-Talismanfragment gefunden."));
		assertNull(FragmentDropAnimationUtility.extractFragmentName(
				"[Legend] Schmiedezustand erhalten: [Sternengeschmiedet]"));
		assertNull(FragmentDropAnimationUtility.extractFragmentName(null));
	}

	@Test
	void splitsNameIntoTitleAndSubtitle() {
		assertArrayEquals(new String[] { "Talismanfragment", "Schmiedekammer" },
				FragmentDropAnimationUtility.splitFragmentName("Schmiedekammer-Talismanfragment"));
		assertArrayEquals(new String[] { "Talismanfragment", "" },
				FragmentDropAnimationUtility.splitFragmentName("Talismanfragment"));
	}
}

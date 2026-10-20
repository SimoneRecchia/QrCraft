package io.github.simonerecchia.qrcraft.qr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class BookTextTest {
	@Test
	void pagesAreJoinedByALineBreak() {
		assertEquals("Line one\nLine two", BookText.join(List.of("Line one", "Line two")));
	}

	@Test
	void blankPagesAndTrailingSpaceGo() {
		assertEquals("first\n\nstill first\nsecond", BookText.join(List.of("  ", "first\n\nstill first  \n", "", "second\n\n")));
	}

	@Test
	void anEmptyBookIsEmpty() {
		assertEquals("", BookText.join(List.of()));
		assertEquals("", BookText.join(List.of("", " \n ")));
	}
}

package de.gupta.xl.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TabColor#of")
final class TabColorTest
{
	@Test
	@DisplayName("parses a 6-digit hex string to RGB components")
	void parsesA6DigitHexStringToRgbComponents()
	{
		var color = TabColor.of("70AD47");

		assertThat(color.red()).as("red").isEqualTo(0x70);
		assertThat(color.green()).as("green").isEqualTo(0xAD);
		assertThat(color.blue()).as("blue").isEqualTo(0x47);
	}

	@Test
	@DisplayName("accepts a leading hash prefix")
	void acceptsALeadingHashPrefix()
	{
		var color = TabColor.of("#70AD47");

		assertThat(color.red()).as("red").isEqualTo(0x70);
	}

	@Test
	@DisplayName("accepts lowercase hex digits")
	void acceptsLowercaseHexDigits()
	{
		var color = TabColor.of("ff0000");

		assertThat(color.red()).as("red").isEqualTo(255);
		assertThat(color.green()).as("green").isZero();
		assertThat(color.blue()).as("blue").isZero();
	}

	@Test
	@DisplayName("throws for an invalid hex string")
	void throwsForAnInvalidHexString()
	{
		assertThatThrownBy(() -> TabColor.of("ZZZZZZ"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("throws for a string that is too short")
	void throwsForAStringThatIsTooShort()
	{
		assertThatThrownBy(() -> TabColor.of("70AD"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
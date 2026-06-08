package de.gupta.xl.application.transfer;

public record CellFormat(
		String numberFormat,
		Boolean bold,
		Boolean italic,
		String fontColorHex,
		String backgroundColorHex
)
{
	public static Builder builder()
	{
		return new Builder();
	}

	public boolean isEmpty()
	{
		return numberFormat == null && bold == null && italic == null
				&& fontColorHex == null && backgroundColorHex == null;
	}

	public static final class Builder
	{
		private String numberFormat;
		private Boolean bold;
		private Boolean italic;
		private String fontColorHex;
		private String backgroundColorHex;

		public Builder numberFormat(final String numberFormat)
		{
			this.numberFormat = numberFormat;
			return this;
		}

		public Builder bold(final Boolean bold)
		{
			this.bold = bold;
			return this;
		}

		public Builder italic(final Boolean italic)
		{
			this.italic = italic;
			return this;
		}

		public Builder fontColor(final String hexRgb)
		{
			this.fontColorHex = hexRgb;
			return this;
		}

		public Builder backgroundColor(final String hexRgb)
		{
			this.backgroundColorHex = hexRgb;
			return this;
		}

		public CellFormat build()
		{
			return new CellFormat(numberFormat, bold, italic, fontColorHex, backgroundColorHex);
		}

		private Builder()
		{
		}
	}
}
package de.gupta.xl.application.transfer;

public record RangeStats(
		long count,
		long numericCount,
		long nonNumericCount,
		Double minimum,
		Double maximum,
		Double mean,
		Double standardDeviation
)
{
	public boolean hasNumericData()
	{
		return numericCount > 0;
	}
}
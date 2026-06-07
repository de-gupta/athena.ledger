module de.gupta.xl.application
{
	requires de.gupta.xl.domain;
	requires de.gupta.aletheia;
	requires de.gupta.athena;

	exports de.gupta.xl.application.port.in;
	exports de.gupta.xl.application.port.out;
	exports de.gupta.xl.application.transfer;
}
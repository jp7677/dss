package eu.europa.esig.dss.asic.xades.signature.opendocument;

import java.io.File;
import java.util.Date;

import javax.xml.crypto.dsig.CanonicalizationMethod;

import org.junit.Before;

import eu.europa.esig.dss.asic.xades.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.signature.DocumentSignatureService;


public class OpenDocumentLevelBInclusiveCanonicalizationTest extends AbstractOpenDocumentTestSignature {
	
	public OpenDocumentLevelBInclusiveCanonicalizationTest(File fileToTest) {
		super(fileToTest);
	}

	private DocumentSignatureService<ASiCWithXAdESSignatureParameters> service;
	private ASiCWithXAdESSignatureParameters signatureParameters;
	
	@Before
	public void init() throws Exception {
		signatureParameters = new ASiCWithXAdESSignatureParameters();
		signatureParameters.bLevel().setSigningDate(new Date());
		signatureParameters.setSigningCertificate(getSigningCert());
		signatureParameters.setSignKeyInfo(true);
		signatureParameters.setCertificateChain(getCertificateChain());
		signatureParameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
		signatureParameters.setSignedInfoCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE);
		signatureParameters.setKeyInfoCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE);
		signatureParameters.setSignedPropertiesCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE);
		signatureParameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);

		service = new ASiCWithXAdESService(getCompleteCertificateVerifier());
		service.setTspSource(getGoodTsa());
	}

	@Override
	protected DocumentSignatureService<ASiCWithXAdESSignatureParameters> getService() {
		return service;
	}

	@Override
	protected ASiCWithXAdESSignatureParameters getSignatureParameters() {
		return signatureParameters;
	}
	
	@Override
	protected String getSigningAlias() {
		return GOOD_USER;
	}
}
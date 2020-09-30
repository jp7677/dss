/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 * 
 * This file is part of the "DSS - Digital Signature Services" project.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package eu.europa.esig.dss.pades.signature.visible.suite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.europa.esig.dss.alert.exception.AlertException;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDigestMatcher;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.test.PKIFactoryAccess;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;

public class PAdESSignatureFieldTest extends PKIFactoryAccess {

	private PAdESService service;
	private PAdESSignatureParameters signatureParameters;

	@BeforeEach
	public void init() throws Exception {

		signatureParameters = new PAdESSignatureParameters();
		signatureParameters.bLevel().setSigningDate(new Date());
		signatureParameters.setSigningCertificate(getSigningCert());
		signatureParameters.setCertificateChain(getCertificateChain());
		signatureParameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);

		SignatureImageParameters imageParameters = new SignatureImageParameters();
		SignatureImageTextParameters textParameters = new SignatureImageTextParameters();
		textParameters.setText("My signature");
		textParameters.setTextColor(Color.GREEN);
		imageParameters.setTextParameters(textParameters);
		signatureParameters.setImageParameters(imageParameters);

		service = new PAdESService(getOfflineCertificateVerifier());
	}

	@Test
	public void testGeneratedTextOnly() throws IOException {

		signatureParameters.getImageParameters().getFieldParameters().setFieldId("Signature1");

		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/doc.pdf"));
		signAndValidate(documentToSign);
	}

	@Test
	public void testSignTwice() throws IOException {

		// Add second field first
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/doc.pdf"));

		SignatureFieldParameters parameters = new SignatureFieldParameters();
		parameters.setFieldId("test");
		parameters.setOriginX(10);
		parameters.setOriginY(10);
		parameters.setHeight(50);
		parameters.setWidth(50);
		DSSDocument doc = service.addNewSignatureField(documentToSign, parameters);
		assertNotNull(doc);

		// Sign twice

		signatureParameters.getImageParameters().getFieldParameters().setFieldId("Signature1");

		DSSDocument doc2 = signAndValidate(doc);
		assertNotNull(doc2);

		signatureParameters.getImageParameters().getFieldParameters().setFieldId("test");

		DSSDocument doc3 = signAndValidate(doc2);
		assertNotNull(doc3);
	}
	
	@Test
	public void testSignTwoFields() throws IOException {

		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/doc.pdf"));
		
		assertEquals(2, countStringOccurance(documentToSign, "startxref"));
		assertEquals(2, countStringOccurance(documentToSign, "%%EOF"));
		
		// add first field
		SignatureFieldParameters parameters = new SignatureFieldParameters();
		parameters.setFieldId("signature1");
		parameters.setOriginX(10);
		parameters.setOriginY(10);
		parameters.setHeight(50);
		parameters.setWidth(50);
		DSSDocument withFirstField = service.addNewSignatureField(documentToSign, parameters);
		assertNotNull(withFirstField);
		
		assertEquals(3, countStringOccurance(withFirstField, "startxref"));
		assertEquals(3, countStringOccurance(withFirstField, "%%EOF"));

		// add second field
		parameters = new SignatureFieldParameters();
		parameters.setFieldId("signature2");
		parameters.setOriginX(100);
		parameters.setOriginY(10);
		parameters.setHeight(50);
		parameters.setWidth(50);
		DSSDocument secondField = service.addNewSignatureField(withFirstField, parameters);
		assertNotNull(secondField);

		assertEquals(4, countStringOccurance(secondField, "startxref"));
		assertEquals(4, countStringOccurance(secondField, "%%EOF"));
		
		// sign first field
		signatureParameters.getImageParameters().getFieldParameters().setFieldId("signature1");
		DSSDocument firstSigned = signAndValidate(secondField);
		assertNotNull(firstSigned);
		
		assertEquals(5, countStringOccurance(firstSigned, "startxref"));
		assertEquals(5, countStringOccurance(firstSigned, "%%EOF"));

		// sign second field
		signatureParameters.getImageParameters().getFieldParameters().setFieldId("signature2");
		DSSDocument secondSigned = signAndValidate(firstSigned);
		assertNotNull(secondSigned);
		
		assertEquals(6, countStringOccurance(secondSigned, "startxref"));
		assertEquals(6, countStringOccurance(secondSigned, "%%EOF"));
		
	}
	
	@Test
	public void createAndSignConsequently() throws IOException {
		
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/doc.pdf"));

		assertEquals(2, countStringOccurance(documentToSign, "startxref"));
		assertEquals(2, countStringOccurance(documentToSign, "%%EOF"));
		
		// add field and sign
		SignatureFieldParameters parameters = new SignatureFieldParameters();
		parameters.setFieldId("signature1");
		parameters.setOriginX(10);
		parameters.setOriginY(10);
		parameters.setHeight(50);
		parameters.setWidth(50);
		DSSDocument withFirstField = service.addNewSignatureField(documentToSign, parameters);
		assertNotNull(withFirstField);

		assertEquals(3, countStringOccurance(withFirstField, "startxref"));
		assertEquals(3, countStringOccurance(withFirstField, "%%EOF"));

		signatureParameters.getImageParameters().getFieldParameters().setFieldId("signature1");
		DSSDocument firstSigned = signAndValidate(withFirstField);
		assertNotNull(firstSigned);

		assertEquals(4, countStringOccurance(firstSigned, "startxref"));
		assertEquals(4, countStringOccurance(firstSigned, "%%EOF"));
		
		// add a new field and second sign
		parameters = new SignatureFieldParameters();
		parameters.setFieldId("signature2");
		parameters.setOriginX(100);
		parameters.setOriginY(10);
		parameters.setHeight(50);
		parameters.setWidth(50);
		DSSDocument secondField = service.addNewSignatureField(firstSigned, parameters);
		assertNotNull(secondField);

		assertEquals(5, countStringOccurance(secondField, "startxref"));
		assertEquals(5, countStringOccurance(secondField, "%%EOF"));

		signatureParameters.getImageParameters().getFieldParameters().setFieldId("signature2");
		DSSDocument secondSigned = signAndValidate(secondField);
		assertNotNull(secondSigned);

		assertEquals(6, countStringOccurance(secondSigned, "startxref"));
		assertEquals(6, countStringOccurance(secondSigned, "%%EOF"));

	}
	
	@Test
	public void createFieldInEmptyDocument() throws IOException {
		
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/EmptyPage.pdf"));
		
		assertEquals(2, countStringOccurance(documentToSign, "startxref"));
		assertEquals(2, countStringOccurance(documentToSign, "%%EOF"));
		
		SignatureFieldParameters parameters = new SignatureFieldParameters();
		parameters.setFieldId("signature1");
		parameters.setOriginX(10);
		parameters.setOriginY(10);
		parameters.setHeight(50);
		parameters.setWidth(50);
		DSSDocument withFirstField = service.addNewSignatureField(documentToSign, parameters);
		assertNotNull(withFirstField);
		
		assertEquals(3, countStringOccurance(withFirstField, "startxref"));
		assertEquals(3, countStringOccurance(withFirstField, "%%EOF"));

		signatureParameters.getImageParameters().getFieldParameters().setFieldId("signature1");
		DSSDocument firstSigned = signAndValidate(withFirstField);
		assertNotNull(firstSigned);
		
		assertEquals(4, countStringOccurance(firstSigned, "startxref"));
		assertEquals(4, countStringOccurance(firstSigned, "%%EOF"));
		
	}

	@Test
	public void testSignTwiceSameField() throws IOException {
		signatureParameters.getImageParameters().getFieldParameters().setFieldId("Signature1");

		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/doc.pdf"));
		DSSDocument doc = signAndValidate(documentToSign);
		assertNotNull(doc);

		assertThrows(DSSException.class, () -> signAndValidate(doc));
	}

	@Test
	public void testFieldNotFound() throws IOException {
		signatureParameters.getImageParameters().getFieldParameters().setFieldId("not-found");

		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/doc.pdf"));
		assertThrows(DSSException.class, () -> signAndValidate(documentToSign));
	}
	
	@Test
	public void fieldsOverlapTest() throws IOException {
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/EmptyPage.pdf"));
		
		SignatureFieldParameters parameters = new SignatureFieldParameters();
		parameters.setFieldId("signature1");
		parameters.setOriginX(10);
		parameters.setOriginY(10);
		parameters.setHeight(50);
		parameters.setWidth(50);
		DSSDocument withFirstField = service.addNewSignatureField(documentToSign, parameters);
		assertNotNull(withFirstField);
		
		parameters.setFieldId("signature2");
		Exception exception = assertThrows(AlertException.class, () -> service.addNewSignatureField(withFirstField, parameters));
		assertEquals("The new signature field position overlaps with an existing annotation!", exception.getMessage());
	}
	
	@Test
	public void differentPagesTest() throws IOException {
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/empty-two-pages.pdf"));
		
		SignatureFieldParameters parameters = new SignatureFieldParameters();
		parameters.setFieldId("signature1");
		parameters.setOriginX(10);
		parameters.setOriginY(10);
		parameters.setHeight(50);
		parameters.setWidth(50);
		parameters.setPage(1);
		DSSDocument withFirstField = service.addNewSignatureField(documentToSign, parameters);
		assertNotNull(withFirstField);
		
		parameters.setFieldId("signature2");
		Exception exception = assertThrows(AlertException.class, () -> service.addNewSignatureField(withFirstField, parameters));
		assertEquals("The new signature field position overlaps with an existing annotation!", exception.getMessage());
		
		parameters.setPage(2);
		DSSDocument withTwoFields = service.addNewSignatureField(withFirstField, parameters);
		
		assertEquals(2, service.getAvailableSignatureFields(withTwoFields).size());
	}
	
	@Test
	public void fieldInsideAnotherTest() throws IOException {
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/EmptyPage.pdf"));
		
		SignatureFieldParameters parametersOne = new SignatureFieldParameters();
		parametersOne.setFieldId("signature1");
		parametersOne.setOriginX(0);
		parametersOne.setOriginY(0);
		parametersOne.setHeight(100);
		parametersOne.setWidth(100);
		DSSDocument withFirstField = service.addNewSignatureField(documentToSign, parametersOne);
		assertNotNull(withFirstField);
		
		SignatureFieldParameters parametersTwo = new SignatureFieldParameters();
		parametersTwo.setOriginX(25);
		parametersTwo.setOriginY(25);
		parametersTwo.setHeight(50);
		parametersTwo.setWidth(50);
		parametersTwo.setFieldId("signature2");
		Exception exception = assertThrows(AlertException.class, () -> service.addNewSignatureField(withFirstField, parametersTwo));
		assertEquals("The new signature field position overlaps with an existing annotation!", exception.getMessage());
	}
	
	@Test
	public void oneCornerOverlapTest() throws IOException {
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/EmptyPage.pdf"));
		
		SignatureFieldParameters parametersOne = new SignatureFieldParameters();
		parametersOne.setFieldId("signature1");
		parametersOne.setOriginX(0);
		parametersOne.setOriginY(0);
		parametersOne.setHeight(100);
		parametersOne.setWidth(100);
		DSSDocument withFirstField = service.addNewSignatureField(documentToSign, parametersOne);
		assertNotNull(withFirstField);
		
		SignatureFieldParameters parametersTwo = new SignatureFieldParameters();
		parametersTwo.setOriginX(75);
		parametersTwo.setOriginY(75);
		parametersTwo.setHeight(100);
		parametersTwo.setWidth(100);
		parametersTwo.setFieldId("signature2");
		Exception exception = assertThrows(AlertException.class, () -> service.addNewSignatureField(withFirstField, parametersTwo));
		assertEquals("The new signature field position overlaps with an existing annotation!", exception.getMessage());
	}
	
	@Test
	public void twoCornersOverlapTest() throws IOException {
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/EmptyPage.pdf"));
		
		SignatureFieldParameters parametersOne = new SignatureFieldParameters();
		parametersOne.setFieldId("signature1");
		parametersOne.setOriginX(0);
		parametersOne.setOriginY(0);
		parametersOne.setHeight(100);
		parametersOne.setWidth(100);
		DSSDocument withFirstField = service.addNewSignatureField(documentToSign, parametersOne);
		assertNotNull(withFirstField);
		
		SignatureFieldParameters parametersTwo = new SignatureFieldParameters();
		parametersTwo.setOriginX(75);
		parametersTwo.setOriginY(25);
		parametersTwo.setHeight(50);
		parametersTwo.setWidth(50);
		parametersTwo.setFieldId("signature2");
		Exception exception = assertThrows(AlertException.class, () -> service.addNewSignatureField(withFirstField, parametersTwo));
		assertEquals("The new signature field position overlaps with an existing annotation!", exception.getMessage());
	}
	
	@Test
	public void sameEdgeTest() throws IOException {
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/EmptyPage.pdf"));
		
		SignatureFieldParameters parametersOne = new SignatureFieldParameters();
		parametersOne.setFieldId("signature1");
		parametersOne.setOriginX(0);
		parametersOne.setOriginY(0);
		parametersOne.setHeight(100);
		parametersOne.setWidth(100);
		DSSDocument withFirstField = service.addNewSignatureField(documentToSign, parametersOne);
		assertNotNull(withFirstField);
		
		SignatureFieldParameters parametersTwo = new SignatureFieldParameters();
		parametersTwo.setOriginX(100);
		parametersTwo.setOriginY(0);
		parametersTwo.setHeight(100);
		parametersTwo.setWidth(100);
		parametersTwo.setFieldId("signature2");
		Exception exception = assertThrows(AlertException.class, () -> service.addNewSignatureField(withFirstField, parametersTwo));
		assertEquals("The new signature field position overlaps with an existing annotation!", exception.getMessage());
		
		parametersTwo.setOriginX(101);
		DSSDocument withTwoFields = service.addNewSignatureField(withFirstField, parametersTwo);
		
		assertEquals(2, service.getAvailableSignatureFields(withTwoFields).size());
	}
	
	@Test
	public void annotationOverlapTest() throws IOException {
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/doc.pdf"));
		
		SignatureFieldParameters parametersOne = new SignatureFieldParameters();
		parametersOne.setFieldId("signature1");
		parametersOne.setOriginX(150);
		parametersOne.setOriginY(150);
		parametersOne.setHeight(100);
		parametersOne.setWidth(100);
		Exception exception = assertThrows(AlertException.class, () -> service.addNewSignatureField(documentToSign, parametersOne));
		assertEquals("The new signature field position overlaps with an existing annotation!", exception.getMessage());
	}
	
	@Test
	public void fieldOverCommentTest() throws IOException {
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/pdf-with-annotations.pdf"));
		
		SignatureFieldParameters parametersOne = new SignatureFieldParameters();
		parametersOne.setFieldId("signature1");
		parametersOne.setOriginX(0);
		parametersOne.setOriginY(0);
		parametersOne.setHeight(50);
		parametersOne.setWidth(50);
		Exception exception = assertThrows(AlertException.class, () -> service.addNewSignatureField(documentToSign, parametersOne));
		assertEquals("The new signature field position overlaps with an existing annotation!", exception.getMessage());
	}
	
	@Test
	public void fieldOverTextNoteTest() throws IOException {
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/pdf-with-annotations.pdf"));
		
		SignatureFieldParameters parametersOne = new SignatureFieldParameters();
		parametersOne.setFieldId("signature1");
		parametersOne.setOriginX(0);
		parametersOne.setOriginY(100);
		parametersOne.setHeight(50);
		parametersOne.setWidth(50);
		Exception exception = assertThrows(AlertException.class, () -> service.addNewSignatureField(documentToSign, parametersOne));
		assertEquals("The new signature field position overlaps with an existing annotation!", exception.getMessage());
	}
	
	@Test
	public void fieldOverDrawingTest() throws IOException {
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/pdf-with-annotations.pdf"));
		
		SignatureFieldParameters parametersOne = new SignatureFieldParameters();
		parametersOne.setFieldId("signature1");
		parametersOne.setOriginX(300);
		parametersOne.setOriginY(25);
		parametersOne.setHeight(50);
		parametersOne.setWidth(50);
		Exception exception = assertThrows(AlertException.class, () -> service.addNewSignatureField(documentToSign, parametersOne));
		assertEquals("The new signature field position overlaps with an existing annotation!", exception.getMessage());
	}
	
	@Test
	public void fieldOverShapeTest() throws IOException {
		DSSDocument documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/pdf-with-annotations.pdf"));
		
		SignatureFieldParameters parametersOne = new SignatureFieldParameters();
		parametersOne.setFieldId("signature1");
		parametersOne.setOriginX(0);
		parametersOne.setOriginY(200);
		parametersOne.setHeight(50);
		parametersOne.setWidth(50);
		Exception exception = assertThrows(AlertException.class, () -> service.addNewSignatureField(documentToSign, parametersOne));
		assertEquals("The new signature field position overlaps with an existing annotation!", exception.getMessage());

		parametersOne.setOriginX(0);
		parametersOne.setOriginY(400);
		DSSDocument document = service.addNewSignatureField(documentToSign, parametersOne);
		assertNotNull(document);
	}

	private DSSDocument signAndValidate(DSSDocument documentToSign) throws IOException {
		ToBeSigned dataToSign = service.getDataToSign(documentToSign, signatureParameters);
		SignatureValue signatureValue = getToken().sign(dataToSign, signatureParameters.getDigestAlgorithm(), getPrivateKeyEntry());
		DSSDocument signedDocument = service.signDocument(documentToSign, signatureParameters, signatureValue);

		// signedDocument.save("target/test.pdf");

		SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(signedDocument);
		validator.setCertificateVerifier(getOfflineCertificateVerifier());
		Reports reports = validator.validateDocument();
		// reports.print();

		DiagnosticData diagnosticData = reports.getDiagnosticData();

		assertTrue(Utils.isCollectionNotEmpty(diagnosticData.getSignatures()));
		for (SignatureWrapper signature : diagnosticData.getSignatures()) {
			assertTrue(signature.isSignatureIntact());
			assertTrue(signature.isSignatureValid());
			assertTrue(Utils.isCollectionNotEmpty(signature.getDigestMatchers()));
			for (XmlDigestMatcher digestMatcher : signature.getDigestMatchers()) {
				assertTrue(digestMatcher.isDataFound());
				assertTrue(digestMatcher.isDataIntact());
			}
		}
		
		assertTrue(diagnosticData.isBLevelTechnicallyValid(diagnosticData.getFirstSignatureId()));

		return signedDocument;
	}
	
	private int countStringOccurance(DSSDocument document, String textToCheck) {
		int counter = 0;
		String line;
		try (InputStream is = document.openStream(); InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr)) {
			while ((line = br.readLine()) != null) {
				if (line.contains(textToCheck)) {
					counter++;
				}
			}
		} catch (Exception e) {
			throw new DSSException(e);
		}
		return counter;
	}

	@Override
	protected String getSigningAlias() {
		return GOOD_USER;
	}

}

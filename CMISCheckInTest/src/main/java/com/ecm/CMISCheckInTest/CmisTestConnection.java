package com.ecm.CMISCheckInTest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisVersioningException;

public class CmisTestConnection {
	static String docID = "idd_B045985F-0000-C61E-B07C-BC257241539A";
	static String user = "P8Admin";
	static String password = "IBMFileNetP8";
	static String url = "http://localhost:9080/cmis11/atom11";
	static String objectStore = "Sales";

	public static void main(String[] args) {
		final Session sess = initSession();
		if(sess != null) {
			try {
				System.out.println("Checking out document: " + docID);
				checkOutDocument(sess);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		if (sess != null) {
			try {
				System.out.println(checkInDocument(sess));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.out.println("Failed to initialize session.");
		}
	}

	private static Session initSession() {
		try {
			SessionFactoryImpl factory = SessionFactoryImpl.newInstance();
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(SessionParameter.USER, user);
			parameters.put(SessionParameter.PASSWORD, password);
			parameters.put(SessionParameter.ATOMPUB_URL, url);
			parameters.put(SessionParameter.REPOSITORY_ID, objectStore);
			parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

			return factory.createSession(parameters);
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
	
	private static void checkOutDocument(Session sess) throws Exception {
		Document latest = sess.getLatestDocumentVersion(docID);
		try {
			latest.checkOut();
		} catch (Exception e) {
			throw e;
		}
	}

	private static String checkInDocument(Session sess) throws Exception {
		CmisObject cmisObject = sess.getObject(docID);
		Document document = (Document) cmisObject;
		cmisObject.refresh(); // contacts the repository and refreshes the
								// object
		Map<String, Object> props = extractProperties(document);
		try {
			InputStream stream = document.getContentStream().getStream();
			String filename = document.getContentStreamFileName();
			String checkedOutID = document.getVersionSeriesCheckedOutId();
			if (checkedOutID == null)
				throw new CmisVersioningException("Document is not currently checked out.");

			if (!document.getId().equals(checkedOutID)) {
				System.out.println("ID does not refer to latest version. Obtaining latest version...");
				document = (Document) sess.getObject(checkedOutID);
			}

			String filetype = "application/octet-stream"; // for example.. Any
															// mime
															// type value can be
															// set
															// here based on the
															// file type.
			// creating a new content stream with new values.
			ContentStream contentStream = sess.getObjectFactory().createContentStream(filename, -1, filetype, stream);
			
			ObjectId nextVer = document.checkIn(true, props, contentStream, "new version");

			return nextVer.getId();

		} catch (Exception ex) {
			// System.out.println("Exception during check-in.");
			throw ex;
		}
	}

	private static Map<String, Object> extractProperties(Document doc) {
		String creator = doc.getCreatedBy();

		// all property values can be accessed by their property ID
		String name = doc.getPropertyValue("cmis:name");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("cmis:objectTypeId", "document");
		properties.put("cmis:name", name);
		properties.put("cmis:createdBy", creator);

		return properties;
	}

}

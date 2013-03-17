package uk.co.jarofgreen.semanticscuttlejar;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Xml;

import org.xml.sax.helpers.DefaultHandler;

import uk.co.jarofgreen.semanticscuttlejar.ScuttleAPIException;


public class APICall  {

	static HttpURLConnection callScuttleURL(String url, Context context) throws IOException {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		String scuttleURL = prefs.getString("url", "");
		if( scuttleURL.length() > 0 ) {
			String last = scuttleURL.substring(scuttleURL.length()-1);
			if( !last.equals("/") ) {
				scuttleURL += "/";
			}
			
			String http = scuttleURL.substring(0,7).toLowerCase();
			String https = scuttleURL.substring(0,8).toLowerCase();
			if (http.compareTo("http://") != 0 && https.compareTo("https://") != 0) {
				scuttleURL = "http://"+scuttleURL;
			}
			
		}
		
		String scuttleUsername = prefs.getString("username", "");
		
		String scuttlePassword = prefs.getString("password", "");
		
		String acceptAllSSLCerts = prefs.getString("acceptAllSSLCerts", "no");
		
        Authenticator.setDefault(new ScuttleAuthenticator(scuttleUsername, scuttlePassword));
        HttpURLConnection c = (HttpURLConnection)(new URL(scuttleURL+url).openConnection());
        c.setUseCaches(false);
        c.setConnectTimeout(1500);
        c.setReadTimeout(300);
        
        
        if (acceptAllSSLCerts.compareTo("yes") == 0) {
	        try {
	        	TrustModifier.relaxHostChecking(c);
	        } catch (KeyStoreException e) {
	        	// 
	        } catch (KeyManagementException e) {
	        	//
	        } catch (NoSuchAlgorithmException e) {
	        	//
	        }
        }
		c.connect();
		return(c);
	}

	static DefaultHandler parseScuttleURL(String url, Context context, DefaultHandler handler) throws ScuttleAPIException {
		try {
			HttpURLConnection c = APICall.callScuttleURL(url, context);
			InputStream is = c.getInputStream();
			String charset = APICall.getConnectionCharset(c);
			Xml.Encoding parse_encoding = Xml.Encoding.ISO_8859_1;
			if ("UTF-8".equals(charset.toUpperCase())) {
				parse_encoding = Xml.Encoding.UTF_8;
			}
			Xml.parse(is, parse_encoding, handler);
		}
		catch( SocketTimeoutException ste ) {
			throw new ScuttleAPIException("Username and/or password is incorrect.");
		} catch( FileNotFoundException fnfe ) {
			throw new ScuttleAPIException("Unable to load URL.  Please check your URL in the Settings.");
		} catch( IOException ioe ) {
			throw new ScuttleAPIException("ioe:"+ioe.getMessage());
		} catch( Exception e ) {
			throw new ScuttleAPIException("e:"+e.getMessage());
		}
		return handler;
	}

	static class ScuttleAuthenticator extends Authenticator {
		private String username, password;
		
		public ScuttleAuthenticator(String user, String pass) {
			this.username = user;
			this.password = pass;
		}
		protected PasswordAuthentication getPasswordAuthentication() {
			return( new PasswordAuthentication(this.username, this.password.toCharArray()) );
		}
	}

	static String getConnectionCharset(HttpURLConnection connection) {
		String contentType = connection.getContentType();
		String[] values = contentType.split(";");
		String charset = "";
		for (String value : values) {
			value = value.trim();
			if (value.toLowerCase().startsWith("charset=")) {
				charset = value.substring("charset=".length());
			}
		}
		if ("".equals(charset)) {
			charset = "UTF-8";
		}
		return charset;
	}
}

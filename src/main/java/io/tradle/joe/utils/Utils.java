package io.tradle.joe.utils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bitcoinj.core.Base58;
import org.h2.security.SHA256;
import org.spongycastle.util.encoders.Hex;

//import io.netty.handler.codec.http.HttpHeaderUtil;

public class Utils {

	public static String getRemoteIPAddress(ChannelHandlerContext ctx) {
		String fullAddress = ((InetSocketAddress) ctx.channel().remoteAddress())
				.getAddress().getHostAddress();

		// Address resolves to /x.x.x.x:zzzz we only want x.x.x.x
		if (fullAddress.startsWith("/")) {
			fullAddress = fullAddress.substring(1);
		}

		int i = fullAddress.indexOf(":");
		if (i != -1) {
			fullAddress = fullAddress.substring(0, i);
		}

		return fullAddress;
	}

	/**
	 * sends GET request to specified uri
	 * @param uri - uri to send GET request to
	 * @return response data in the form of an integer code and string response
	 */
	public static HttpResponseData get(URI uri) {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(uri);
		HttpResponse response = null;
		String respStr = null;
		int code = -1;
		try {
			response = client.execute(request);
			code = response.getStatusLine().getStatusCode();

			// Get the response
			StringBuilder respSB = new StringBuilder();
			String line;
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			while ((line = rd.readLine()) != null) {
				respSB.append(line);
			}

			respStr = respSB.toString();
		} catch (IOException i) {
			respStr = i.getMessage();
		}

		return new HttpResponseData(code, respStr);
	}
	
	/**
	 * @param postReq - POST request
	 * @return map of parameters to values
	 */
	public static Map<String, String> getPOSTRequestParameters(HttpRequest postReq) {
		if (!postReq.getMethod().equals(HttpMethod.POST))
			throw new IllegalArgumentException("argument must be POST request");
		
		HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(postReq);
		List<InterfaceHttpData> datas = decoder.getBodyHttpDatas();
		Map<String, String> params = getQueryParams(postReq);
		for (InterfaceHttpData data : datas) {
			if (data.getHttpDataType() == HttpDataType.Attribute) {
				Attribute attribute = (Attribute) data;
				try {
					params.put(attribute.getName(), attribute.getValue());
				} catch (IOException i) {
					// should never happen but...
					throw new IllegalStateException("Failed to read POST data", i);
				}
			}
		}
		
		return params;
	}

	/**
	 * parses GET or POST request into param->value map
	 * @param req - GET or POST request
	 * @return param->value map
	 */
	public static Map<String, String> getRequestParameters(HttpRequest req) {
		if (req.getMethod().equals(HttpMethod.POST))
			return getPOSTRequestParameters(req);
		
		return getQueryParams(req);
	}

	private static Map<String, String> getQueryParams(HttpRequest req) {
		Map<String, String> simplifiedParams = new HashMap<String, String>();
		Map<String, List<String>> originalParams = new QueryStringDecoder(req.getUri()).parameters();
		if (originalParams != null) {
			Set<String> paramNames = originalParams.keySet();
			for (String paramName: paramNames) {
				simplifiedParams.put(paramName, originalParams.get(paramName).get(0));
			}
		}
		
		return simplifiedParams;
	}

	public static String toBase64String(byte[] bytes) {
		return new String(Base64.encodeBase64(bytes));
	}

	public static String toBase58String(byte[] bytes) {
		return Base58.encode(bytes);
	}
	
	public static boolean arrayStartsWith(byte[] a, byte[] b) {
		return rangeEquals(a, 0, b, 0, b.length);
	}

	public static boolean rangeEquals(byte[] a, int aStart, byte[] b, int bStart, int length) {
	    assert a.length - aStart > length && b.length - bStart > length;

	    for (int i = aStart, j = bStart, k = 0; k < length; k++) {
	        if (a[i] != b[j])
	            return false;
	    }
	    
	    return true;
	}

	public static boolean isTruthy(String s) {
		return !Utils.isFalsy(s);
	}
	
	public static boolean isFalsy(String s) {
		if (s == null)
			return true;
		
		s = s.toLowerCase();
		return s.equals("0") || 
			   s.equals("false") ||
			   s.equals("n") ||
			   s.equals("no");
	}
}

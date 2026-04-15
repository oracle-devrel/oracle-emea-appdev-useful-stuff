package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;

import jakarta.inject.Singleton;

@Singleton
/**
 * do some form of handling of the normalized data.
 * 
 * The handlerCore will construct a chain of handlers based on it's
 * configuration, identifying them by matching the name returned by the
 * getHandlerName() method.
 * 
 * It is possible for a handler to be in more than once chain, but this will be
 * cause a failure UNLESS isStateless() returns true.
 * 
 */
public interface NormalizedDataMessageHandler extends Comparable<NormalizedDataMessageHandler> {

	/**
	 * If transforming the input should not modify the input object, but instead
	 * create a new version(s), the resulting objects will be passed to the next
	 * stage in the order they are presented in the response..
	 * 
	 * This approach allows a transform to split up (or otherwise enrich) the data,
	 * OR to combine data, for example taking several individual inputs representing
	 * individual elements and once all elements of a resulting object have been
	 * provided then generating a single object combining the inputs in some way as
	 * an output(s).
	 * 
	 * This can be used as a filter by returning only objects that pass the filter
	 * 
	 * For output the result could be all outputs, or perhaps limited to onl'y
	 * objects that were not uploaded (this is application specific)
	 * 
	 * While this can throw an exception (in which case it will be treated as if it
	 * had returned an empty result set) the best practice is for it to handle
	 * exceptions and just return an empty array.
	 * 
	 * @param input
	 * @return
	 */
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception;

	/**
	 * returns the place the handler sits in the chain, this should be retrieved
	 * from configuration and must not be static
	 * 
	 * @return
	 */
	public int getOrder();

	/**
	 * returns the name of the handler. Useful for diagnostics
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * returns the configuration of the handler. Useful for diagnostics
	 * 
	 * @return
	 */
	public String getConfig();

	@Override
	public default int compareTo(NormalizedDataMessageHandler otherNormalizedDataMessageHandler) {
		if (otherNormalizedDataMessageHandler == null) {
			throw new NullPointerException("otherNormalizedDataMessageHandler must not be null");
		}
		return Integer.compare(otherNormalizedDataMessageHandler.getOrder(), this.getOrder());
	}
}
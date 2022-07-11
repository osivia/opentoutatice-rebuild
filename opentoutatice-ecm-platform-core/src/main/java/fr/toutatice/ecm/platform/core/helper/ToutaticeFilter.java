package fr.toutatice.ecm.platform.core.helper;

import java.io.Serializable;

public interface ToutaticeFilter<T> extends Serializable{
	
	    /**
	     * Filters data models objects.
	     *
	     * @return true if accepting the object false otherwise
	     */
	    public boolean accept(T t);
	
}

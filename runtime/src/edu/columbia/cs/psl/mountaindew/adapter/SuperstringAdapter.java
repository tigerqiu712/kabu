package edu.columbia.cs.psl.mountaindew.adapter;

import superstring.Superstring;

public class SuperstringAdapter extends DefaultAdapter {
	
	@Override
	public Object adaptOutput(Object model, Object...testData) {
		Class objClass = model.getClass();
		
		if (objClass.equals(Superstring.class)) {
			Superstring s = (Superstring)model;
			return s.getSolution();
		}
		return model;
	}

}

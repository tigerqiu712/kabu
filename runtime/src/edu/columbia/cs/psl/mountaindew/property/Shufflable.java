package edu.columbia.cs.psl.mountaindew.property;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import edu.columbia.cs.psl.metamorphic.struct.MethodInvocation;
import edu.columbia.cs.psl.metamorphic.struct.Variable;
import edu.columbia.cs.psl.mountaindew.property.MetamorphicProperty.PropertyResult.Result;

public class Shufflable extends PairwiseMetamorphicProperty {

	@Override
	public String getName() {
		return "Shuffleable";
	}
	@Override
	public boolean propertyApplies() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	protected boolean returnValuesApply(Object p1, Object returnValue1,
			Object p2, Object returnValue2) {
		return returnValue1.equals(returnValue2);
	}

	@Override
	protected boolean propertyApplies(MethodInvocation i1, MethodInvocation i2, int interestedVariable) {
		Object o1 = i1.params[interestedVariable].value;
		Object o2 = i2.params[interestedVariable].value;
		for(int i = 0;i<i1.params.length;i++)
			if(i!=interestedVariable && !i1.params[i].value.equals(i2.params[i].value))
				return false;
		
		if(o1.getClass().isArray() && o2.getClass().isArray())
		{
			if(Array.getLength(o1) != Array.getLength(o2))
				return false;
			HashSet<Object> o1h = new HashSet<Object>();
			HashSet<Object> o2h = new HashSet<Object>();
			for(int i = 0;i<Array.getLength(o1);i++)
			{
				o1h.add(Array.get(o1, i));
				o2h.add(Array.get(o2, i));
			}
			return o1h.equals(o2h);
		}
		else if(Collection.class.isAssignableFrom(o1.getClass()) && Collection.class.isAssignableFrom(o2.getClass()))
		{
			HashSet<Object> o1h = new HashSet<Object>();
			HashSet<Object> o2h = new HashSet<Object>();
			o1h.addAll((Collection) o1);
			o2h.addAll((Collection) o2);
			return o1h.equals(o2h);
		}
		return false;
	}

	@Override
	protected int[] getInterestedVariableIndices() {
		ArrayList<Integer> rets = new ArrayList<Integer>();
		for(int i = 0;i<getMethod().getParameterTypes().length; i++)
		{
			if(getMethod().getParameterTypes()[i].isArray() || Collection.class.isAssignableFrom(getMethod().getParameterTypes()[i]))
				rets.add(i);
		}
		int[] ret = new int[rets.size()];
		for(int i = 0;i<rets.size();i++)
			ret[i]=rets.get(i);
		return ret;
	}

}
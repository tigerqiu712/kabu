package edu.columbia.cs.psl.mountaindew.property;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

import edu.columbia.cs.psl.invivo.struct.MethodInvocation;
import edu.columbia.cs.psl.metamorphic.inputProcessor.MetamorphicInputProcessor;
import edu.columbia.cs.psl.metamorphic.inputProcessor.impl.InclusiveMin;

public class InclusiveByMin extends ClusiveAbstract {

	@Override
	protected boolean returnValuesApply(Object p1, Object returnValue1,
			Object p2, Object returnValue2) {
		// TODO Auto-generated method stub
		double rt1Min, rt1Sum, rt2Min, rt2Sum;
		if (returnValue1.getClass().isArray() && returnValue2.getClass().isArray()) {
			rt1Min = this.findMin(returnValue1);
			rt1Sum = this.calSum(returnValue1);
			
			rt2Min = this.findMin(returnValue2);
			rt2Sum = this.calSum(returnValue2);
			
			/*System.out.println("rt1Min: " + rt1Min);
			System.out.println("rt2Min: " + rt2Min);
			System.out.println("rt1Sum: " + rt1Sum);
			System.out.println("rt2Sum: " + rt2Sum);*/
			
			if (rt1Min - rt2Min == 1 && rt2Sum - rt1Sum == rt2Min)
				return true;
		}
		
		System.out.println("Returvalue1 class: " + returnValue1.getClass().getName() + " " + Collection.class.isAssignableFrom(returnValue1.getClass()));
		System.out.println("Returvalue1 class: " + returnValue1.getClass().getName() + " " + Collection.class.isAssignableFrom(returnValue2.getClass()));
		
		if (Collection.class.isAssignableFrom(returnValue1.getClass()) && Collection.class.isAssignableFrom(returnValue2.getClass())) {
			rt1Min = this.findMin(returnValue1);
			rt1Sum = this.calSum(returnValue1);
			
			rt2Min = this.findMin(returnValue2);
			rt2Sum = this.calSum(returnValue2);
			
			/*System.out.println("rt1Min: " + rt1Min);
			System.out.println("rt2Min: " + rt2Min);
			System.out.println("rt1Sum: " + rt1Sum);
			System.out.println("rt2Sum: " + rt2Sum);*/
			
			if (rt1Min - rt2Min == 1 && rt2Sum - rt1Sum == rt2Min)
				return true;
		}

		return false;
	}

	@Override
	protected boolean propertyApplies(MethodInvocation i1, MethodInvocation i2,
			int interestedVariable) {
		// TODO Auto-generated method stub
		//Get specific parameter from parameters by interestedVariable
		Object o1 = i1.params[interestedVariable];
		Object o2 = i2.params[interestedVariable];
		for (int i = 0 ; i < i2.params.length; i++) {
			if (i != interestedVariable && i1.params[i] != i2.params[i]) {
				return false;
			}
		}
				
		//If parameter is array or collection, check length if i2 = i1 +1, check sum
		if (o1.getClass().isArray() && o2.getClass().isArray()) {
			int o1Length = Array.getLength(o1);
			int o2Length = Array.getLength(o2);
			if (o1Length + 1 != o2Length)
				return false;
					
			double o1Min = this.findMin(o1);
			double o1Sum = this.calSum(o1);
					
			double o2Min = this.findMin(o2);
			double o2Sum = this.calSum(o2);
					
			/*System.out.println("DEBUG inclusiveByMin array: o1Min" + o1Min);
			System.out.println("DEBUG inclusiveByMin array: o2Min" + o2Min);
			System.out.println("DEBUG inclusiveByMin array: o1Sum" + o1Sum);
			System.out.println("DEBUG inclusiveByMin array: o2Sum" + o2Sum);*/
					
			if (o1Min - 1 == o2Min && o2Sum - o1Sum == o2Min) {
				System.out.println("It's true");
				return true;
			}
		} else if (Collection.class.isAssignableFrom(o1.getClass()) && (Collection.class.isAssignableFrom(o2.getClass()))) {
			int o1Size = ((Collection)o1).size();
			int o2Size = ((Collection)o2).size();
					
			if (o1Size + 1 != o2Size)
				return false;
					
			double o1Min = this.findMin(o1);
			double o1Sum = this.calSum(o1);
					
			double o2Min = this.findMin(o2);
			double o2Sum = this.calSum(o2);
					
			/*System.out.println("DEBUG inclusiveByMin list: o1Min" + o1Min);
			System.out.println("DEBUG inclusiveByMin list: o2Min" + o2Min);
			System.out.println("DEBUG inclusiveByMin list: o1Sum" + o1Sum);
			System.out.println("DEBUG inclusiveByMin list: o2Sum" + o2Sum);*/
					
			if (o1Min - 1 == o2Min && o2Sum - o1Sum == o2Min)
				return true;
		}
		
		return false;
	}
	
	private double findMin(Object arrayList) {
		double min = Double.MAX_VALUE;
		double tmp;
		
		if (arrayList.getClass().isArray()) {
			int arrayLength = Array.getLength(arrayList);
			
			for (int i = 0; i < arrayLength; i++) {
				tmp = ((Number)Array.get(arrayList, i)).doubleValue();
				
				if (tmp < min) {
					min = tmp;
				}
			}
			
			return min;
		} else if (Collection.class.isAssignableFrom(arrayList.getClass())) {
			Iterator colIT = ((Collection)arrayList).iterator();
			while(colIT.hasNext()) {
				tmp = ((Number)colIT.next()).doubleValue();
				
				if (tmp < min) {
					min = tmp;
				} 
			}
			
			return min;
		}
		throw new IllegalArgumentException("Only allow array and collection to find max");
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "InclusiveByMin";
	}

	@Override
	public MetamorphicInputProcessor getInputProcessor() {
		// TODO Auto-generated method stub
		return new InclusiveMin();
	}

}
package com.amd.aparapi.internal.writer;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import com.amd.aparapi.internal.model.ClassModel;

public abstract class ScalaParameter {
	public static enum DIRECTION {
		IN, OUT
	}

	protected final String type;
	protected final String name;
	protected final Class<?> clazz;
	protected final DIRECTION dir;
	protected final List<String> typeParameterDescs;
	protected final List<Boolean> typeParameterIsObject;

	public ScalaParameter(String fullSig, String name, DIRECTION dir) {
		this.name = name;
		this.clazz = null;
		this.dir = dir;

		this.typeParameterDescs = new LinkedList<String>();
		this.typeParameterIsObject = new LinkedList<Boolean>();

		String eleSig = null;
		if (fullSig.charAt(0) == '[')
			eleSig = fullSig;
		else
			eleSig = fullSig.substring(1);

		if (eleSig.indexOf('<') != -1) {
			String topLevelType = eleSig.substring(0, eleSig.indexOf('<'));
			if (topLevelType.charAt(0) == 'L')
				this.type = topLevelType.substring(1).replace('/', '.');
			else
				this.type = topLevelType.replace('/', '.');

			String params = eleSig.substring(eleSig.indexOf('<') + 1, eleSig.lastIndexOf('>'));
			if (params.indexOf('<') != -1 || params.indexOf('>') != -1)
				throw new RuntimeException("Do not support nested parameter templates: " + fullSig);
			String[] tokens = params.split(",");
			for (int i = 0; i < tokens.length; i++) {
				String t = tokens[i];
				if (t.equals("I") || t.equals("F") || t.equals("D")) {
					this.typeParameterDescs.add(t);
					this.typeParameterIsObject.add(false);
				} else {
					this.typeParameterDescs.add("L" + t.replace('/', '.') + ";");
					this.typeParameterIsObject.add(true);
				}
			}
		} else {
			if (eleSig.equals("I"))
				this.type = "int";
			else if (eleSig.equals("D"))
				this.type = "double";
			else if (eleSig.equals("F"))
				this.type = "float";
			else if (eleSig.startsWith("L"))
				this.type = eleSig.substring(1, eleSig.length() - 1).replace('/', '.');
			else
				throw new RuntimeException("Invalid type: " + eleSig);
		}
	}

	public ScalaParameter(String type, Class<?> clazz, String name, DIRECTION dir) {
		this.type = type.trim();
		this.clazz = clazz;
		this.name = name;
		this.dir = dir;
		this.typeParameterDescs = new LinkedList<String>();
		this.typeParameterIsObject = new LinkedList<Boolean>();
	}

	public ScalaParameter(String fullSig, String name) {
		this(fullSig, name, DIRECTION.IN);
	}

	public void addTypeParameter(String s, boolean isObject) {
		typeParameterDescs.add(s);
		typeParameterIsObject.add(isObject);
	}

	public String[] getDescArray() {
		String[] arr = new String[typeParameterDescs.size()];
		int index = 0;
		for (String param : typeParameterDescs) {
			arr[index] = param;
			index++;
		}
		return arr;
	}

	public String getTypeParameter(int i) {
		if (i < typeParameterDescs.size())
			return typeParameterDescs.get(i);
		else
			return null;
	}

	public boolean typeParameterIsObject(int i) {
		if (i < typeParameterIsObject.size())
			return typeParameterIsObject.get(i);
		return false;
	}

	public String getType() {
		StringBuilder sb = new StringBuilder();
		sb.append(type.replace('.', '_'));
		for (String typeParam : typeParameterDescs) {
			sb.append("_");
			if (typeParam.charAt(0) == 'L') {
				sb.append(typeParam.substring(1,
                  typeParam.length() - 1).replace(".", "_"));
			} else
				sb.append(typeParam);
		}
		return sb.toString();
	}

	protected String getParameterStringFor(KernelWriter writer, int field) {
		final String param;
		if (!typeParameterIsObject(field)) {
			param = "__global " + ClassModel.convert(
			          typeParameterDescs.get(field), "", true) + "* " + name + "_" + (field + 1);
		} else {
			String fieldDesc = typeParameterDescs.get(field);
			if (fieldDesc.charAt(0) != 'L' ||
			    fieldDesc.charAt(fieldDesc.length() - 1) != ';')
				throw new RuntimeException("Invalid object signature \"" + fieldDesc + "\"");
			fieldDesc = fieldDesc.substring(1, fieldDesc.length() - 1);
			param = "__global " + fieldDesc.replace('.', '_') + "* " + name +
			        "_" + (field + 1);
		}
		return param;
	}

	public String getName() {
		return name;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public DIRECTION getDir() {
		return dir; 
	}

	public abstract String getInputParameterString(KernelWriter writer);
	public abstract String getOutputParameterString(KernelWriter writer);
	public abstract String getStructString(KernelWriter writer);
	public abstract String getAssignString(KernelWriter writer);
}


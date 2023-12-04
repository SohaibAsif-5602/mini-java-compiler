import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *	The <strong>JavaClassFileGenerator</strong> was created in cooperation with the University of Applied Sciences Aachen as a bachelor thesis project. 
 *	Its original purpose is to be used during a course teaching fundamentals of formal languages and compilers. </br> </br> 
 * 
 *  The <strong>JavaClassFileGenerator</strong> constructor takes 4 parameters; the first is a String representing the output file name, followed by 3 boolean values that, 
 *  if set to <code>true</code> will generate the (1) class file, (2) a file containing class file's hexadecimal code as a formatted String, and 
 *  (3) the same file as (2) but with commoents as to what each set of bytes represents. The readable file (2) can be placed into a 
 *  text editor with hexadecimal encoding which will in turn create the same output as (1). </br> </br>
 *  
 *  To generate the files an instance of the <strong>JavaClassFileGenerator</strong> needs to be created and its only public method <code>generateClassFile(MethodObject...)</code> 
 *  invoked passing any number of <strong>MethodObject</strong> objects (see below) and one with the name "main" which will be the execution entry point for the JVM as 
 *  known from standard java class files. The main method's parameters value will always be overwritten to 1 (one) therefore any int value is accepted. </br> </br>
 *  
 *  To make a call to a separate function, your byte code will need to use the opcode for <code>invokestatic</code> followed by the name of the method in parentheses 
 *  <em>( e.g. </em><code>b8 (print)</code><em> )</em>.  </br>
 *  For easy output to the console a static "print" method is included with every compilation of code and can be invoked as any other method passing a single parameter to 
 *  be written to the console. </br> </br>
 *  
 *  The <strong>MethodObject</strong> is used to pass a single method to the <strong>JavaClassFileGenerator</strong>. It takes 3 parameters; (1) String: name of the method, 
 *  (2) int: number of parameters, (3) String: byte code as hexadecimal bytes. All white-space characters in the byte code will be ignored. Remember that at least one 
 *  <strong>MethodObject</strong> with name "main" needs to be passed to successfully create a class file. </br> </br>
 *  <strong>IMPORTANT:</strong> Only int variables/constants are allowed inside the byte code! <em>(version 1.0.0)</em> </br>
 *  
 *  Hint: After compilation of the class file you can inspect it using the JDK command "<kbd>javap -v [filename]</kbd>". </br>
 * 
 * @version 1.0.5
 */
public class JavaClassFileGenerator {
	private static final String LINESEPARATOR = System.lineSeparator();

	/* input parameters for file output*/
	private final boolean binary;
	private final boolean readable;
	private final boolean verbose;

	/* final fields for the general class file creation */
	private final String magicNumber = "cafe babe 0000 0034";
	private final String access_flags = "0021";
	private int thisReference = 1;
	private int superReference = 3;
	private final String interfacesTable = "0000";
	private final String attributesTable = "0000";

	/* fields for the creation of the methods table */
	private ArrayList<Method> methodsTable;
	private static int mainClassIndex;
	private static Set<String> methodNames;

	/* fields for the creation of the constants table */
	private static ArrayList<Constant> constantsList;
	private static int codeIndex;
	private static int printFieldIndex;
	private static int printMethodIndex;
	private static int stackMapTableIndex;

	/* fields for the creation of the fields table */
	private static ArrayList<Field> fieldsTable;
	
	/* OPCODES */
	public final static String BIPUSH = "10";
	public final static String SIPUSH = "11";
	public final static String ILOAD = "15";
	public final static String ISTORE = "36";
	public final static String IADD = "60";
	public final static String ISUB = "64";
	public final static String IMUL = "68";
	public final static String IDIV = "6c";
	public final static String IF_ICMPEQ = "9f";
	public final static String IF_ICMPNE = "a0";
	public final static String IF_ICMPLT = "a1";
	public final static String IF_ICMPGE = "a2";
	public final static String IF_ICMPGT = "a3";
	public final static String IF_ICMPLE = "a4";
	public final static String GOTO = "a7";
	public final static String IRETURN = "ac";
	public final static String RETURN = "b1";
	public final static String GETSTATIC = "b2";
	public final static String PUTSTATIC = "b3";
	public final static String INVOKEVIRTUAL = "b6";
	public final static String INVOKESTATIC = "b8";

	/**
	 * The only implemented constants table values are: <br>
	 * <ul><li>UTF-8</li><li>Class reference</li><li>Field reference</li><li>Name and type descriptor</li><li>Method reference</li></ul>
	 */
	public final static String[] constantTypes =
		{"Utf8", "Class", "Fieldref", "NameAndType", "Methodref"};

	protected final String fileName;
	private static Hashtable<String, Integer> globalVariables;

	/** This function will create a byte array from the hexadecimal code passed as a String object
	 * @param s
	 * The String containing a hexadecimal representation of the byte code.
	 * @return
	 * <em>byte[]</em> The converted hexadecimal code representation as a byte array.
	 */
	private static byte[] hexStringToByteArray(String s) {
		if (s.length() % 2 != 0) {
			System.err.println("Bytestream has uneven number of hexvalues and is therefore corrupt.");
			System.exit(-1);
		}
		int len = s.length();
		byte[] ret = new byte[len/2];
		for (int i=0; i<len; i+=2) {
			ret[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4 ) + Character.digit(s.charAt(i+1), 16));
		}
		return ret;
	}

	/** Creating an instance of this class will allow you to use the <code>generateClassFile</code> method which will scan 
	 * the byte code passed to it and create the actual files as defined in the parameters below.
	 * If no output type is set to true, the method will not execute.
	 * @param fileName </br>
	 * <em>(String)</em> The class and filename of the generated files
	 * @param binary </br>
	 * <em>(boolean)</em> If true, will create an executable java class file 
	 * @param readable </br>
	 * <em>(boolean)</em> If true, will create a readable text file with formatting for easy modification when using a text editor that is capable of hexadecimal encoding.
	 * @param verbose </br>
	 * <em>(boolean)</em> If true, will create a verbose text file with the same formatting as the readable file and in addition has comments for all byte sequences.
	 * @since 1.0.0
	 */
	public JavaClassFileGenerator(String fileName, boolean binary, boolean readable, boolean verbose) {
		if (!binary && !readable && !verbose) {
			System.out.println("No output type selected.");
			System.exit(-1);
		}
		this.binary = binary;
		this.readable = readable;
		this.verbose = verbose;
		constantsList  = new ArrayList<>();
		methodsTable = new ArrayList<>();
		methodNames = new HashSet<>();
		globalVariables = new Hashtable<>();
		fieldsTable = new ArrayList<>();
		this.fileName = fileName;

		/* Create default constants table */
		ConstantsTable.addConstant("Class", "0002", fileName);
		ConstantsTable.addConstant("Utf8", fileName, fileName);
		ConstantsTable.addConstant("Class", "0004", "java/lang/Object");
		ConstantsTable.addConstant("Utf8", "java/lang/Object", "java/lang/Object");
		codeIndex = ConstantsTable.addUTF("Code");
		printFieldIndex = ConstantsTable.addFieldReference("java/lang/System", "out", "Ljava/io/PrintStream;");
		printMethodIndex = ConstantsTable.addMethodReference("java/io/PrintStream", "println", "(I)V");
		stackMapTableIndex = ConstantsTable.addUTF("StackMapTable");
		ConstantsTable.addClassReference("[Ljava/lang/String;");
		ConstantsTable.addMethodReference(this.fileName, "print", "(I)V");
	}

	/**
	 * This method will scan all methods and their byte codes and generate the all overhead needed for an executable and functional java class file. 
	 * @param methodsList </br>
	 * <em>(MethodObject...)</em> Takes an array or sequence of MethodObjects of any length but at least one method with the name "<em>main</em>" must be passed. 
	 * Also a method by the name "<em>print</em>" is already defined inside the class which allows to print an argument to the console by simply invoking it. <br>
	 * To invoke any method from the byte code you will need to use the opcode for <code>invokestatic</code> followed by the method name in parentheses. E.g.: <code>b8 (print)</code>
	 */
	public void generateClassFile(MethodObject... methodsList) {
		if (methodsList.length < 1) {
			System.out.println("No methods passed.");
			System.exit(-1);
		}

		/* create internal print method for ease of use */
		methodsTable.add(new Method("print", ConstantsTable.addUTF("print"), ConstantsTable.addUTF("(I)V"), 1, 
				GETSTATIC + String.format("%04x", printFieldIndex) + ILOAD + "00" + INVOKEVIRTUAL + String.format("%04x", printMethodIndex) + RETURN));

		/* iterate through all passed methods and save the indexes of name and descriptor, 
		 * and create the local methods. */
		boolean mainExists = false;
		for (int i=0; i<methodsList.length; i++) {
			String bc = methodsList[i].getByteCode().replaceAll("\\s+", "");
			//String bc = methodsList[i].getByteCode().replaceAll("\\s+", "").toLowerCase();
			int nIndex, dIndex;
			if (methodsList[i].getName().equals("main")) {
				mainExists = true;
				mainClassIndex = i+1;
				nIndex = ConstantsTable.addUTF("main");
				dIndex = ConstantsTable.addUTF("([Ljava/lang/String;)V");
				methodsTable.add(new Method("main", nIndex, dIndex, 1, bc));
			} else {
				nIndex = ConstantsTable.addUTF(methodsList[i].getName());
				String returnType;
				if (bc.substring(bc.length()-2).equals(RETURN))
					returnType = ")V";
				else
					returnType = ")I";
				String descriptor = "(";
				for (int j=0; j<methodsList[i].getArgsSize(); j++) {
					descriptor += "I";
				}
				descriptor += returnType;
				dIndex = ConstantsTable.addUTF(descriptor);
				ConstantsTable.addMethodReference(fileName, methodsList[i].getName(), descriptor);
				methodsTable.add(new Method(methodsList[i].getName(), nIndex, dIndex, methodsList[i].getArgsSize(), bc));
			}
		}
		if (!mainExists) {
			System.out.println("Main method not found.");
			System.exit(-1);
		}

		/* Note all global Variables from the main procedure */
		methodsTable.get(mainClassIndex).defineGlobalVariables();

		/* Scan the code and replace methodCalls with correct address */
		for (Method m: methodsTable) {
			for (String s: methodNames) {
				if (m.byteCode.contains("("+ s +")")) {
					int index = ConstantsTable.getMethodReferenceIndex(s);
					m.byteCode = m.byteCode.replaceAll("\\("+s+"\\)", String.format("%04x", index));
				}
			}
			globalVariables.forEach((k, v) -> {
				m.byteCode = m.byteCode.replaceAll("\\["+k+"\\]", String.format("%08x", v).substring(4,8));
			});
			m.scanByteCode();
		}

		// Put everything to String
		if (binary) {
			StringBuilder hex = new StringBuilder();
			hex.append(magicNumber.replaceAll("\\s+", ""));
			hex.append(ConstantsTable.getByteCode());
			hex.append(access_flags);
			hex.append(String.format("%04x", thisReference));
			hex.append(String.format("%04x", superReference));
			hex.append(interfacesTable);

			hex.append(String.format("%04x", fieldsTable.size()));
			for (Field f: fieldsTable) {
				hex.append(f.getBinaryCode());
			}

			hex.append(String.format("%04x", methodsTable.size()));
			for (Method m: methodsTable) {
				hex.append(m.getBinaryCode());
			}
			hex.append(attributesTable);

			byte[] bc = hexStringToByteArray(hex.toString());

			try {
				OutputStream os = new FileOutputStream(fileName+".class");
				os.write(bc);
				os.close();
			} catch (Exception e) {
				System.out.println("Binary file creation failed");
				System.exit(-1);
			}
			System.out.println("Binary file \"" +fileName+ ".class\" created.");
		}

		if (readable) {
			StringBuilder read = new StringBuilder();
			read.append(magicNumber+LINESEPARATOR);
			read.append(ConstantsTable.getReadableCode());
			read.append(access_flags + " ");
			read.append(String.format("%04x ", thisReference));
			read.append(String.format("%04x%n", superReference));
			read.append(interfacesTable+LINESEPARATOR);

			read.append(String.format("%04x%n", fieldsTable.size()));
			for (Field f: fieldsTable) {
				read.append(f.getReadableCode());
			}

			read.append(String.format("%04x%n", methodsTable.size()));
			for (Method m: methodsTable) {
				read.append(m.getReadableCode());
			}
			read.append(attributesTable);

			PrintWriter out;
			try {
				out = new PrintWriter(fileName+"R.txt");
				out.print(read);
				out.close();
			} catch (Exception e) {
				System.out.println("readable file creation failed");
				System.exit(-1);
			}
			System.out.println("Readable file \"" +fileName+ "R.txt\" created.");
		}

		if (verbose) {
			StringBuilder verb = new StringBuilder();
			verb.append(":::magic number + minor and major version:::" + LINESEPARATOR);
			verb.append(magicNumber+LINESEPARATOR);
			verb.append(LINESEPARATOR+":::constants table:::"+LINESEPARATOR);
			verb.append(ConstantsTable.getVerboseCode());
			verb.append(LINESEPARATOR+":::class access flags, this class index, super class index:::"+LINESEPARATOR);
			verb.append(String.format("%s %04x %04x%n", access_flags, thisReference, superReference));
			verb.append(LINESEPARATOR+":::interfaces table:::"+LINESEPARATOR);
			verb.append(interfacesTable+LINESEPARATOR);
			verb.append(LINESEPARATOR+":::fields table:::"+LINESEPARATOR);
			verb.append(String.format("%04x  // fields table size%n%n", fieldsTable.size()));
			for (Field f: fieldsTable) {
				verb.append(f.getVerboseCode());
			}
			verb.append(LINESEPARATOR+":::methods table:::"+LINESEPARATOR);
			verb.append(String.format("%04x  // methods table size%n%n", methodsTable.size()));
			for (Method m: methodsTable) {
				verb.append(m.getVerboseCode());
			}
			verb.append(LINESEPARATOR+":::attributes table:::"+LINESEPARATOR);
			verb.append(attributesTable + "  // attribute table size" + LINESEPARATOR);

			PrintWriter out;
			try {
				out = new PrintWriter(fileName+"V.txt");
				out.print(verb);
				out.close();
			} catch (Exception e) {
				System.out.println("Verbose file creation failed");
				System.exit(-1);
			}
			System.out.println("Verbose file \"" +fileName+ "V.txt\" created.");
		}
	}

	
	private class Field {
		private String name;
		private String accessFlags;
		private int nameIndex;
		private int descriptorIndex;
		private String attributes;

		Field(String name) {
			this.name = name;
			this.accessFlags = "0008"; // static
			this.nameIndex = ConstantsTable.addUTF(name);
			this.descriptorIndex = ConstantsTable.addUTF("I");
			this.attributes = "0000";
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Field))
				return false;
			Field f = (Field) o;
			return this.name.equals(f.name);
		}

		public String getBinaryCode() {
			StringBuilder ret = new StringBuilder();
			ret.append(this.accessFlags);
			ret.append(String.format("%04x", nameIndex));
			ret.append(String.format("%04x", descriptorIndex));
			ret.append(attributes);
			return ret.toString();
		}

		public String getReadableCode() {
			StringBuilder ret = new StringBuilder();
			ret.append("0008"+LINESEPARATOR);
			ret.append(String.format("%04x%n", nameIndex));
			ret.append(String.format("%04x%n", descriptorIndex));
			ret.append(attributes+LINESEPARATOR);
			return ret.toString();
		}

		public String getVerboseCode() {
			StringBuilder ret = new StringBuilder();
			ret.append("0008 // access flags" + LINESEPARATOR);
			ret.append(String.format("%04x // \"%s\" name index%n", nameIndex, name));
			ret.append(String.format("%04x // \"I\" descriptor index%n", descriptorIndex));
			ret.append(attributes + " // field attributes table");
			return ret.toString();
		}
	}

	private class Method {
		private String byteCode;
		private String binaryString; 
		private String readableString;
		private String verboseString;
		private Attribute stackMapTable;
		private String accessFlags;
		private String name;
		private int nameIndex;
		private int descriptorIndex;
		private int stack;
		private int maxStack = 1;
		private int maxLocals;

		private final String[] jumpCodes = {IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, GOTO, IF_ICMPEQ, "99", "9c", "9d", "9e", "9b", "9a", "c7", "c6"};
		private final String[] skipOne = {BIPUSH, ILOAD, ISTORE, "12", "a9"}; // bipush, iload, istore, ldc, ret
		private final String[] skipTwo = {"84", GETSTATIC, INVOKEVIRTUAL, "b7", INVOKESTATIC, SIPUSH}; // iinc, getstatic, invokevirtual, invokespecial, invokestatic, sipush
		private final String[] stackInc = {BIPUSH, SIPUSH, "12", ILOAD, GETSTATIC}; // bipush, sipush, ldc, iload, getstatic
		private final String[] stackDec = {ISTORE, IADD, ISUB, IMUL, IDIV}; // istore, iadd, isub, imul, idiv
		private final String[] locals = {ILOAD, ISTORE};

		public Method(String name, int nameIndex, int descriptorIndex, int argsSize, String byteCode) {
			this.accessFlags = "0009";
			this.name = name;
			this.nameIndex = nameIndex;
			this.descriptorIndex = descriptorIndex;
			this.maxLocals = argsSize;
			this.byteCode = byteCode;
			methodNames.add(this.name);
		}

		private void createBinaryCode() {
			StringBuilder ret = new StringBuilder();
			ret.append(accessFlags);
			ret.append(String.format("%04x", nameIndex));
			ret.append(String.format("%04x", descriptorIndex));
			ret.append("0001"); // MethodAttributesTable
			ret.append(String.format("%04x", codeIndex));
			ret.append(String.format("%08x", getCodeAttributeLength()));
			ret.append(String.format("%04x", maxStack));
			ret.append(String.format("%04x", maxLocals));
			ret.append(String.format("%08x", byteCode.length()/2));
			ret.append(byteCode);
			ret.append("0000"); // ExceptionsTable
			if (null == stackMapTable)
				ret.append("0000");
			else {
				ret.append("0001");
				ret.append(String.format("%04x", stackMapTableIndex));
				ret.append(String.format("%08x", stackMapTable.code.size()+2)); // entries(2)
				ret.append(String.format("%04x", stackMapTable.entries));
				for (String s: stackMapTable.code) {
					ret.append(s);
				}
			}

			binaryString = ret.toString();
		}

		private void createReadableCode() {
			StringBuilder ret = new StringBuilder();
			ret.append(accessFlags+LINESEPARATOR);
			ret.append(String.format("%04x%n", nameIndex));
			ret.append(String.format("%04x%n", descriptorIndex));
			ret.append("0001"+LINESEPARATOR); // MethodAttributesTable
			ret.append(String.format("%04x%n", codeIndex));
			ret.append(String.format("%08x%n", getCodeAttributeLength()));
			ret.append(String.format("%04x%n", maxStack));
			ret.append(String.format("%04x%n", maxLocals));
			ret.append(String.format("%08x%n", byteCode.length()/2));
			for (int i=0; i<byteCode.length(); i+=2) {
				switch(byteCode.substring(i,i+2)) {
				case BIPUSH:
				case ILOAD:
				case ISTORE:
					ret.append(String.format("%s %s%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+4)));
					i+=2;
					break;
				case IF_ICMPEQ:
				case IF_ICMPNE:
				case IF_ICMPLT:
				case IF_ICMPGE:
				case IF_ICMPGT:
				case IF_ICMPLE:
				case GOTO:
				case GETSTATIC:
				case INVOKEVIRTUAL:
				case INVOKESTATIC:
					ret.append(String.format("%s %s%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+6)));
					i+=4;
					break;
				case IADD:
				case ISUB:
				case IMUL:
				case IDIV:
				case IRETURN:
				case RETURN:
				default:
					ret.append(String.format("%s%n", byteCode.substring(i,i+2)));
				}
			}
			ret.append("0000"+LINESEPARATOR); // ExceptionsTable
			if (null == stackMapTable)
				ret.append("0000"+LINESEPARATOR);
			else {
				ret.append("0001"+LINESEPARATOR);
				ret.append(String.format("%04x%n", stackMapTableIndex));
				ret.append(String.format("%08x%n", stackMapTable.code.size()+2)); // entries(2)
				ret.append(String.format("%04x%n", stackMapTable.entries));
				for (String s: stackMapTable.code) {
					ret.append(s+" ");
				}
				ret.append(LINESEPARATOR);
			}
			ret.append(LINESEPARATOR);
			readableString = ret.toString();
		}

		private void createVerboseCode() {
			StringBuilder ret = new StringBuilder();
			ret.append("::: Method \"" + name + "\" :::" + LINESEPARATOR);
			ret.append(String.format("%s  // access flags%n", accessFlags));
			ret.append(String.format("%04x  // name index%n", nameIndex));
			ret.append(String.format("%04x  // descriptor index%n", descriptorIndex));
			ret.append("0001  // method's attributes table"+LINESEPARATOR); // MethodAttributesTable
			ret.append(String.format("%04x  // \"Code\" index%n", codeIndex));
			ret.append(String.format("%08x  // attribute's length%n", getCodeAttributeLength()));
			ret.append(String.format("%04x  // stack size%n", maxStack));
			ret.append(String.format("%04x  // locals%n", maxLocals));
			if (this.name.equals("print"))
				ret.append(String.format("%08x  // bytecode size%n", byteCode.length()/2));
			else
				ret.append(String.format("%08x  // bytecode size%n%n****Your byte code for \""+ name +"\" starts here****%n", byteCode.length()/2));
			for (int i=0; i<byteCode.length(); i+=2) {
				switch(byteCode.substring(i,i+2)) {
				case BIPUSH:
					ret.append(String.format("%s %s  - bipush%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+4)));
					i+=2;
					break;
				case SIPUSH:
					ret.append(String.format("%s %s- sipush%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+6)));
					i+=4;
					break;
				case ILOAD:
					ret.append(String.format("%s %s  - iload%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+4)));
					i+=2;
					break;
				case ISTORE:
					ret.append(String.format("%s %s  - istore%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+4)));
					i+=2;
					break;
				case IADD:
					ret.append(IADD + "     - iadd"+LINESEPARATOR);
					break;
				case ISUB:
					ret.append(ISUB + "     - isub"+LINESEPARATOR);
					break;
				case IMUL:
					ret.append(IMUL + "     - imul"+LINESEPARATOR);
					break;
				case IDIV:
					ret.append(IDIV + "     - idiv"+LINESEPARATOR);
					break;
				case IF_ICMPEQ:
					ret.append(String.format("%s %s  - if_icmpeq%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+6)));
					i+=4;
					break;
				case IF_ICMPNE:
					ret.append(String.format("%s %s  - if_icmpne%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+6)));
					i+=4;
					break;
				case IF_ICMPLT:
					ret.append(String.format("%s %s  - if_icmplt%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+6)));
					i+=4;
					break;
				case IF_ICMPGE:
					ret.append(String.format("%s %s  - if_icmpge%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+6)));
					i+=4;
					break;
				case IF_ICMPGT:
					ret.append(String.format("%s %s  - if_icmpgt%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+6)));
					i+=4;
					break;
				case IF_ICMPLE:
					ret.append(String.format("%s %s  - if_icmple%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+6)));
					i+=4;
					break;
				case GOTO:
					ret.append(String.format("%s %s  - goto%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+6)));
					i+=4;
					break;
				case IRETURN:
					ret.append(IRETURN + "     - ireturn"+LINESEPARATOR);
					break;
				case RETURN:
					ret.append(RETURN + "     - return"+LINESEPARATOR);
					break;
				case GETSTATIC:
					ret.append(String.format("%s %s  - getstatic%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+6)));
					i+=4;
					break;
				case PUTSTATIC:
					ret.append(String.format("%s %s  - putstatic%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+6)));
					i+=4;
					break;
				case INVOKEVIRTUAL:
					ret.append(String.format("%s %s  - invokevirtual%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+6)));
					i+=4;
					break;
				case INVOKESTATIC:
					ret.append(String.format("%s %s  - invokestatic%n", byteCode.substring(i,i+2), byteCode.substring(i+2,i+6)));
					i+=4;
					break;
				default:
					ret.append(String.format("%s ***Illegal OPCODE***%n", byteCode.substring(i,i+2)));
				}
			}
			ret.append(LINESEPARATOR+"0000  // exceptions table"+LINESEPARATOR); // ExceptionsTable
			if (null == stackMapTable)
				ret.append("0000  // attribute's attribute table"+LINESEPARATOR);
			else {
				ret.append("0001  // attribute's attribute table"+LINESEPARATOR);
				ret.append(String.format("%04x  // \"StackMapTable\" index%n", stackMapTableIndex));
				ret.append(String.format("%08x  // attribute size%n", stackMapTable.code.size()+2)); // entries(2)
				ret.append(String.format("%04x  // StackMapTable entries%n", stackMapTable.entries));
				for (String s: stackMapTable.code) {
					ret.append(s+" ");
				}
				ret.append(" // attribute values necessary for goto and compare operations"+LINESEPARATOR);
			}
			ret.append(LINESEPARATOR);
			verboseString = ret.toString();
		}

		private int getCodeAttributeLength() {
			int ret = 2+2+4+2+2+byteCode.length()/2;
			if (null != stackMapTable)
				ret += stackMapTable.code.size()+2+4+2;
			return ret;
		}

		public void defineGlobalVariables() {
			// recognize following String "b3[constantName]" and extract the characters inside the brackets
			// whitespace is removed beforehand 
			Pattern p = Pattern.compile("(?<=b3\\[)[a-zA-Z]\\w*(?=\\])"); 
			Matcher m = p.matcher(byteCode);
			while (m.find()) {
				String id = m.group();
				globalVariables.put(id, ConstantsTable.addFieldReference(fileName, id, "I"));
				Field f = new Field(id);
				if (!fieldsTable.contains(f)) {
					fieldsTable.add(f);
				}
			}
		}

		private void scanByteCode() {

			ArrayList<Jump> jumpArray = new ArrayList<>();
			ArrayList<String> jumpCodesTable = new ArrayList<>(Arrays.asList(jumpCodes));
			ArrayList<String> skipOneTable = new ArrayList<>(Arrays.asList(skipOne));
			ArrayList<String> skipTwoTable = new ArrayList<>(Arrays.asList(skipTwo));
			ArrayList<String> stackIncTable = new ArrayList<>(Arrays.asList(stackInc));
			ArrayList<String> stackDecTable = new ArrayList<>(Arrays.asList(stackDec));
			ArrayList<String> localsTable = new ArrayList<>(Arrays.asList(locals));

			for (int i=0; i<byteCode.length(); i+=2) {
				String temp = byteCode.substring(i, i+2).toLowerCase();
				if (jumpCodesTable.contains(temp)) {
					String s = byteCode.substring(i+2,i+6);
					int offset = Integer.parseInt(s, 16);
					if (offset > 32767)
						offset -= 65536;
					Jump jtemp = new Jump((int) i/2, offset, (int) i/2+offset);
					if (!jumpArray.contains(jtemp))
						jumpArray.add(jtemp);
					
					i+=4;
					continue;
				}
				if (stackIncTable.contains(temp)) {
					stack++;
					maxStack = maxStack < stack ? stack : maxStack;
				} else if (stackDecTable.contains(temp))
					stack--;
				if (localsTable.contains(temp)) {
					String l = byteCode.substring(i+2,i+4);
					if (Integer.parseInt(l, 16)+1 > maxLocals)
						maxLocals = Integer.parseInt(l, 16)+1;
				}
				if (skipOneTable.contains(temp))
					i+=2;
				else if (skipTwoTable.contains(temp))
					i+=4;
			}
			if (jumpArray.size() > 0) {
				Collections.sort(jumpArray,  new Comparator<Jump>() {
					@Override
					public int compare(Jump j1, Jump j2) {
						return j1.target < j2.target ? -1 : 0;
					}
				});
				stackMapTable = new Attribute(stackMapTableIndex);
				stackMapTable.entries = jumpArray.size();
				stackMapTable.code.add("ff");
				stackMapTable.code.add(String.format("%04x", jumpArray.get(0).target).substring(0,2)); // offset
				stackMapTable.code.add(String.format("%04x", jumpArray.get(0).target).substring(2));
				stackMapTable.code.add(String.format("%04x", maxLocals).substring(0,2)); // Number of locals
				stackMapTable.code.add(String.format("%04x", maxLocals).substring(2));
				if (name == "main") {
					int cr = ConstantsTable.addClassReference("[Ljava/lang/String;");
					stackMapTable.code.add("07");
					stackMapTable.code.add(String.format("%04x", cr).substring(0,2));
					stackMapTable.code.add(String.format("%04x", cr).substring(2));
				} else if (this.maxLocals > 0) {
					stackMapTable.code.add("01");
				}
				for (int i = 1; i < maxLocals; i++) {
					stackMapTable.code.add("01");
				}
				stackMapTable.code.add("00");
				stackMapTable.code.add("00");
				for (int i = 1; i < jumpArray.size(); i++) {
					stackMapTable.code.add(String.format("%02x", jumpArray.get(i).target-jumpArray.get(i-1).target-1));
				}
			}
		}

		public String getBinaryCode() {
			if (binaryString == null)
				createBinaryCode();
			return binaryString;
		}

		public String getReadableCode() {
			if (readableString == null)
				createReadableCode();
			return readableString;
		}

		public String getVerboseCode() {
			if (verboseString == null)
				createVerboseCode();
			return verboseString;
		}
	}


	class Jump {
		// start and offset are not used/neccessary
		
		private int start;
		private int offset;
		int target;

		Jump(int start, int offset, int target) {
			this.start = start;
			this.offset = offset;
			this.target = target;
		}

		@Override
		public String toString() {
			return String.format("Start: %d, Offset: %d, Target: %d", start, offset, target);
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Jump)) 
				return false;
			Jump j = (Jump) o;
			return this.target == j.target;
		}
	}

	class Attribute {
		private int name_index;
		int entries;
		ArrayList<String> code;

		Attribute(int index) {
			name_index = index;
			entries = 0;
			code = new ArrayList<>();
		}

		String attributeToHex() {
			StringBuilder ret = new StringBuilder();
			ret.append(String.format("%04x%n", name_index));
			ret.append(String.format("%08x%n", code.size()+2)); // entries(2)
			ret.append(String.format("%04x%n", entries));
			for (String s: code) {
				ret.append(s+LINESEPARATOR);
			}
			return ret.toString();
		}

		int getAttributeCodeLength() {
			return code.size() + 8; // "StackMapTable"(2) + length (4) + entries (2) 
		}
	}

	static class ConstantsTable {

		private static int addConstant(String type, String value, String id) {
			if (!Arrays.asList(constantTypes).contains(type)) {
				System.out.println("Invalid type!");
				System.exit(-1);
			}
			Constant constant = new Constant(type, value, id);
			int pos = constantsList.indexOf(constant);
			if (pos < 0) {
				constantsList.add(constant);
				return constantsList.size();
			}
			return pos+1;
		}

		private static int getMethodReferenceIndex(String s) {
			Constant constant = new Constant("Methodref", "", s);
			int pos = constantsList.indexOf(constant);
			if (pos >= 0)
				return pos+1;
			System.out.println("No matching Method found");
			System.exit(-1);
			return -1;
		}

		private static int addUTF(String s) {
			return addConstant("Utf8", s, s);
		}

		private static int addClassReference(String className) {
			int pos = constantsList.indexOf(new Constant("Methodref", "", className));
			if (pos < 0)
				return addConstant("Class", Integer.toString(addUTF(className)), className);
			return pos;
		}

		private static int addNameAndType(String id, String type) {
			return addConstant("NameAndType", Integer.toString(addUTF(id))+":"+Integer.toString(addUTF(type)), id+":"+type);
		}

		private static int addMethodReference(String classRef, String id, String type) {
			return addConstant("Methodref", Integer.toString(addClassReference(classRef))+"."+Integer.toString(addNameAndType(id, type)), id);
		}

		private static int addFieldReference(String classRef, String id, String type) {
			return addConstant("Fieldref", Integer.toString(addClassReference(classRef))+"."+Integer.toString(addNameAndType(id, type)), id);
		}

		private static String getByteCode() {
			StringBuilder byteCode = new StringBuilder();
			byteCode.append(String.format("%04x", constantsList.size()+1));
			for (Constant c:constantsList) {
				String[] s;
				switch(c.type) {
				case "Utf8":
					byteCode.append(String.format("01%04x%s", c.value.length(), convertToHexString(c.value).replaceAll("\\s+",  "")));
					break;
				case "Class":
					byteCode.append(String.format("07%04x", Integer.parseInt(c.value,10)));
					break;
				case "Fieldref":
					s = c.value.split("\\.");
					byteCode.append(String.format("09%04x%04x", Integer.parseInt(s[0],10), Integer.parseInt(s[1],10)));
					break;
				case "Methodref":
					s = c.value.split("\\.");
					byteCode.append(String.format("0a%04x%04x", Integer.parseInt(s[0],10), Integer.parseInt(s[1],10)));
					break;
				case "NameAndType":
					s = c.value.split(":");
					byteCode.append(String.format("0c%04x%04x", Integer.parseInt(s[0],10), Integer.parseInt(s[1],10)));
					break;
				case "Integer":
					byteCode.append(String.format("03%08x", Integer.parseInt(c.value)));
					break;
				}
			}
			return byteCode.toString();
		}

		private static String getReadableCode() {
			StringBuilder byteCode = new StringBuilder();
			byteCode.append(String.format("%04x%n", constantsList.size()+1));
			for (Constant c:constantsList) {
				String[] s;
				switch(c.type) {
				case "Utf8":
					byteCode.append(String.format("01 %04x  %s%n", c.value.length(), convertToHexString(c.value)));
					break;
				case "Class":
					byteCode.append(String.format("07 %04x%n", Integer.parseInt(c.value,10)));
					break;
				case "Fieldref":
					s = c.value.split("\\.");
					byteCode.append(String.format("09 %04x %04x%n", Integer.parseInt(s[0],10), Integer.parseInt(s[1],10)));
					break;
				case "Methodref":
					s = c.value.split("\\.");
					byteCode.append(String.format("0a %04x %04x%n", Integer.parseInt(s[0],10), Integer.parseInt(s[1],10)));
					break;
				case "NameAndType":
					s = c.value.split(":");
					byteCode.append(String.format("0c %04x %04x%n", Integer.parseInt(s[0],10), Integer.parseInt(s[1],10)));
					break;
				case "Integer":
					byteCode.append(String.format("03  %08x%n", Integer.parseInt(c.value)));
					break;
				}
			}
			return byteCode.toString();
		}

		private static String getVerboseCode() {
			StringBuilder byteCode = new StringBuilder();
			byteCode.append(String.format("%04x  // constants table size%n", constantsList.size()+1));
			int line = 0;
			for (Constant c:constantsList) {
				byteCode.append(String.format("%3d: ", ++line));
				String[] s;
				switch(c.type) {
				case "Utf8":
					byteCode.append(String.format("01 %04x  %s - Constant \"%s\"%n", c.value.length(), convertToHexString(c.value), c.value));
					break;
				case "Class":
					byteCode.append(String.format("07 %04x - Class @%d%n", Integer.parseInt(c.value,10), Integer.parseInt(c.value)));
					break;
				case "Fieldref":
					s = c.value.split("\\.");
					byteCode.append(String.format("09 %04x %04x - Field reference - class:@%d, NameAndType:@%d%n", Integer.parseInt(s[0],10), Integer.parseInt(s[1],10), Integer.parseInt(s[0],10), Integer.parseInt(s[1],10)));
					break;
				case "Methodref":
					s = c.value.split("\\.");
					byteCode.append(String.format("0a %04x %04x - Method reference - class:@%d, NameAndType:@%d%n", Integer.parseInt(s[0],10), Integer.parseInt(s[1],10), Integer.parseInt(s[0],10), Integer.parseInt(s[1],10)));
					break;
				case "NameAndType":
					s = c.value.split(":");
					byteCode.append(String.format("0c %04x %04x - Name and type - name:@%d, type:@%d%n", Integer.parseInt(s[0],10), Integer.parseInt(s[1],10), Integer.parseInt(s[0],10), Integer.parseInt(s[1],10)));
					break;
				}
			}
			return byteCode.toString();
		}

		private static String convertToHexString(String s) {
			String ret = "";
			char[] chars = s.toCharArray();
			for (char c: chars)
				ret += Integer.toHexString(c) + " ";
			return ret;
		}
	}
}

/**
 * The Constants class is used for entries in the constants table. The type argument holds information about the type of entry, the value is a string that
 * can hold several types of info depending on the type of entry this is. For further information see the java class file documentation. The id holds the name
 * that the entry can be referenced by.
 * 
 * @author Tommy
 *
 */
class Constant {
	String type;
	String value;
	String id;

	/**
	 * @param type A constants table entry type. For further information see the Java class file documentation.
	 * @param value A String that can hold several types of information depending on the type of entry. For further information see the Java class file documentation.
	 * @param id A String that is used to find this specific entry in the constants table.
	 */
	Constant(String type, String value, String id) {
		this.type = type;
		this.value = value;
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Constant))
			return false;
		Constant c = (Constant) o;
		return this.id.equals(c.id) && this.type.contentEquals(c.type);
	}
}
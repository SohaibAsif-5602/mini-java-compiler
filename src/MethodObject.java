/**
 * This object is needed to pass the byte code to the <strong>JavaClassFileGenerator</strong>. Its arguments are 
 * (1) String: method name, (2) int: number of parameters, and (3) String: hexadecimal representation of the java byte code. </br>  
 * Remember that you <strong>must</strong> pass at least one method with the name "<em>main</em>" to the 
 * <strong>JavaClassFileGenerator</strong> and since a "<em>print</em>" method already exists and duplicates are not 
 * allowed, therefore you must not pass a method with that name.
 *
 * @version 1.0.0
 */
public class MethodObject {
	private String name;
	private int argsSize;
	private String byteCode;
	
	/**
	 * The </strong>MethodObject</strong> constructor.
	 * @param name
	 * <em>String</em>  </br>
	 * method name
	 * @param argsSize <em>int</em> </br> 
	 * number of parameters for this method.  </br><em>Note</em>: the main methods argument size must be 1, but this value will be overwritten and therefore can be any value 
	 * @param byteCode <em>String</em> </br>
	 *  the hexadecimal representation of byte code 
	 */
	public MethodObject(String name, int argsSize, String byteCode) {
		this.name = name;
		this.argsSize = argsSize;
		this.byteCode = byteCode;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getArgsSize() {
		return argsSize;
	}

	public void setArgsSize(int argsSize) {
		this.argsSize = argsSize;
	}

	public String getByteCode() {
		return byteCode;
	}

	public void setByteCode(String byteCode) {
		this.byteCode = byteCode;
	}
}

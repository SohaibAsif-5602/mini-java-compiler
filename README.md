# MiniJava Compiler

## Overview
This is a simple compiler for `MiniJava`, a minimalistic Java-like programming language. The compiler is implemented in `JavaCC` and was built as part of the "Compilerbau" course at FH Aachen. It can be used to compile `MiniJava` source code into `Java` bytecode.

## Usage

### Using the included JAR
1. Ensure you have `Java` installed on your system.
2. Open a terminal and navigate to the `compiler` directory containing the `MiniJava.jar` file (or get the release [here](https://github.com/BlackyDrum/mini-java-compiler/releases)).
3. Run the following command to compile a `MiniJava` source file:
```
$ java -jar MiniJava.jar < mySourceFile.txt
```
5. The compiler will generate a `Java` class file named `MiniJavaClassFile.class`. Run the compiled class file with the following command:
```
$ java MiniJavaClassFile
```

### Compiling the source code manually
1. Ensure you have `Java` and `JavaCC` installed on your system.
2. Open a terminal and navigate to the `src` directory.
3. Run the following command to generate the `Java` parser:
```
$ javacc MiniJava.jj
```
4. Compile the generated `Java` files:
```
$ javac MiniJava.java
```
5. Run the following command to compile a `MiniJava` source file:
```
$ java MiniJava < mySourceFile.txt
```
6. The compiler will generate a `Java` class file named `MiniJavaClassFile.class`. Run the compiled class file with the following command:
```
$ java MiniJavaClassFile
```

## Sample MiniJava Code
**Example 1:**
```java
final int n = 10;

func fib(int n) {
        int result = 0;
        if n <= 1 {
                result = n;
        }
        else {
                result = fib(n - 1) + fib(n - 2);
        }
	return (result);
}

static void main() {
    print(fib(n));
}
```

**Example 2:**
```java
final int number = 12345;

func getRightmostDigit(int number) {
	int n;
	{
		n = number;
		
		while n >= 10 {
			n = n - 10;
		}
	}

	return (n);
}

func removeRightmostDigit(int number) {
	{}
	return (number / 10);
}

func reverse(int number) {
	int reversedNumber = 0, remainingNumber, digit;
	{
		remainingNumber = number;

		while remainingNumber != 0 {
			digit = getRightmostDigit(remainingNumber);
			reversedNumber = reversedNumber * 10 + digit;
			remainingNumber = removeRightmostDigit(remainingNumber);
		}
	}
	return (reversedNumber);
}

static void main() {
    print(reverse(number));
}
```

**Example 3:**
```java
final int x = 14400;

// Note: The result is rounded down to the nearest integer, and this method works accurately only
// for numbers with integer square roots, because the MiniJavaCompiler only works with integers

func sqrt(int x) {
	int start = 1, end, result, mid;

	{
		if x == 0 result = 0;
		else if x == 1 result = 1;

		end = x;
		
		while start <= end {
			mid = (start + end) / 2;

			if mid * mid == x {
				result = mid;
			}

			if mid * mid < x {
				start = mid + 1;
				result = mid;
			}
			else {
				end = mid - 1;
			}
		}
		
	}

	return (result);
}

static void main() {
	print(sqrt(x));
}
```

**Example 4:**
```java
final int a = 3, b = 2;
int i;

void myPrint(int n) {
	print(n);
}

func ack(int a, int b) {
        int ret = 0;
        if b == 0
	{
		ret = 1;
        } 
	else
		if a == 0 
		{
            		if b == 1 ret = 2;
                	else ret = b + 2;
        	} 
		else 
		{
        		ret = ack(a - 1, ack(a,b-1));
        	}
        return ret;
}

static void main() {
	while i < 10 {
		myPrint(ack(a,b));
		i = i + 1;
	}
}
```

## LL(1) Grammar Analyzer
I have also built a calculator for generating `first`, `follow`, and `lookahead` sets to check if a grammar is `LL(1)`. You can find the calculator [here](https://blackydrum.github.io/first-follow-ll1-calculator/).
Feel free to explore and use it in conjunction with the `MiniJava` compiler. The `MiniJava` language grammar used by the compiler is also available in this repository. <br> <br>
**Please note that the provided grammar is not `LL(1)` due to the presence of the 'else' keyword in the `<optElse>` production, which introduces ambiguity. This is because after seeing an `if` statement, the parser cannot determine whether to expand `<optElse>` to `else` `<statement>` or to `epsilon`.**




現在select文のみ

### SQL
| id | name |
----|---- 
| 1 | example |
| 2 | john |

### Code
~~~
public static void main() throws IOException, SQLException {
	final SQLManager sqlManager = SQLManager.INSTANCE;
	sqlManager.setup();

	final Example example = new Example();
	sqlManager.select(example);

	System.out.println("id: " + example.getId());
	System.out.println("name: " + example.getName());
}

class Example extends QueryObject {
	
	@Select
	private int id;
	@Select
	@Sif(type = SifType.EQUALS, value = "example")
	private String name;

	public Example() {
		super("accounts");
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

}
~~~

### Result
~~~
id: 1
name: example
~~~
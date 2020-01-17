
# this is garbage. これはゴミです。

##### 筆跡時 ver0.2.0

Select 1.0
Insert 1.0
Update 0
Delete 0

### SQL
##### accounts

| id | name | admin |
----|----|----
| 1 | example | 1
| 2 | john | 0

##### chats

| id | user_id | msg |
----|----|----
| 1 | 1 | hello
| 2 | 1 | world

### Code
~~~
public static void main() throws Exception {
	try (final SQLLib sqlLib = SQLManager.INSTANCE.setup()) {
		final Example example = new Example();
		sqlManager.select(example);

		System.out.println("id: " + example.getId());
		System.out.println("name: " + example.getName());
		System.out.println("admin: " + example.isAdmin());
		System.out.println("chats: " + example.getChats().toString());
	}
}

@Table("accounts")
class Example extends QueryObject {
	
	@Select
	@Insert
	private int id;
	@Select
	@Sif(type = SifType.EQUALS, value = "example")
	@Insert
	private String name;
	@Select
	@Insert(update = true) // update = deplicate update
	@Converter(BooleanConverter.class)
	private boolean admin;
	@Select
	private List<ChatLog> chats;

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isAdmin() {
		return admin;
	}

	public List<ChatLog> getChats() {
		return chats;
	}

}

@Table("chats")
@TSame(origin = "id", column = "user_id")
class ChatLog extends QueryObject {
	@Select
	@Insert
	private String msg;

	@Override
	public String toString() {
		return msg;
	}
}
~~~

### Result
~~~
id: 1
name: example
admin: true
chats: [hello, world]
~~~

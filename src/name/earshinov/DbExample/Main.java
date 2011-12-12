package name.earshinov.DbExample;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

public class Main {

	public static final String USAGE =
		"Использование: java name.eashinov.DbExample.Main <КОМАНДА> <АРГУМЕНТЫ КОМАНДЫ>\n" +
		"\n" +
		"Доступные команды:\n" +
		"\n" +
		"    list-all - показать все записи из базы\n" +
		"    list-by-empno [EMPNO [...]] - показать записи с заданными EMPNO\n" +
		"    insert-with-duplicate EMPNO ENAME JOB_TITLE\n" +
		"\n" +
		"Команда insert-with-duplicate добавляет в базу заданного Employee, а также\n" +
		"запись-дубликат с EMPNO, увеличенным на 1, ссылающуюся на первую запись\n" +
		"посредством внешнего ключа DUPLICATE_EMPNO.";

	private static final String DERBY_DRIVER = "org.apache.derby.jdbc.ClientDriver";
	private static final String CONNECTIONS_STRING = "jdbc:derby://localhost:1527/Lesson22";

	// Консольный интерфейс к программе
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println(USAGE);
			System.exit(1);
		}

		final String command = args[0];
		final List<String> remainingArgs = Arrays.asList(args).subList(1, args.length);
		try {
			if (command.equals("list-all"))
				executeListAll(remainingArgs);
			else if (command.equals("list-by-empno"))
				executeListByEmpno(remainingArgs);
			else if (command.equals("insert-with-duplicate"))
				executeInsertWithDuplicate(remainingArgs);
			else {
				System.err.println("Неизвестная команда: " + command);
				System.err.println(USAGE);
				System.exit(1);
			}
		}
		catch (CommandException e) {
			System.err.println("Ошибка при выполнении команды " + command);
			System.err.println(e.getMessage());
			System.err.println();
			System.err.println(USAGE);
			System.exit(1);
		}
		catch (Throwable e) {
			System.err.println("Ошибка при выполнении команды " + command);
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	// Реализация команды list-all.
	// Демонстрация использования Statement.
	// Выводит все записи из базы
	private static void executeListAll(List<String> commandArgs)
		throws CommandException {

		if (commandArgs.size() > 0)
			throw new CommandException("Команда list-all не принимает аргументы");

		// подключение драйвера Derby на случай, если используется не сановская JDK
		try {
			Class.forName(DERBY_DRIVER);
		}
		catch (ClassNotFoundException e) {
			throw new CommandException("Could not load Derby JDBC driver \"" + DERBY_DRIVER + "\"");
		}

		try {
			Connection conn = null;
			Statement st = null;
			try {
				conn = DriverManager.getConnection(CONNECTIONS_STRING);
				st = conn.createStatement();
				printEmployees(st.executeQuery("SELECT * FROM Employee"));
			}
			finally {
				if (st != null) st.close();
				if (conn != null) conn.close();
			}
		}
		catch (SQLException e) {
			throw new RuntimeException("SQLException: " + e.getMessage(), e);
		}
	}

	// Реализация команды list-by-empno.
	// Демонстрация использования Prepared Statement.
	// Выводит записи из базы с empno, заданными в аргументах.
	//
	// На самом деле это не очень удачный пример использования Prepared Statement:
	// легче единожды использовать простой Statement с инструкцией IN,
	// чем несколько раз -- Prepared Statement с операцией `=`.  Вот только метода
	// для экранирования строки в SQL-запросе JDBC по-видимому не предоставляет:
	// <http://stackoverflow.com/questions/6680595/java-sql-escape-without-using-setstring>
	//
	private static void executeListByEmpno(List<String> commandArgs)
		throws CommandException {

		// подключение драйвера Derby на случай, если используется не сановская JDK
		try {
			Class.forName(DERBY_DRIVER);
		}
		catch (ClassNotFoundException e) {
			throw new CommandException("Could not load Derby JDBC driver \"" + DERBY_DRIVER + "\"");
		}

		try {
			Connection conn = null;
			PreparedStatement st = null;
			try {
				conn = DriverManager.getConnection(CONNECTIONS_STRING);
				st = conn.prepareStatement("SELECT * FROM Employee WHERE empno = ?");
				for (String empnoString : commandArgs) {
					int empno;
					try {
						empno = Integer.parseInt(empnoString);
					}
					catch (NumberFormatException e) {
						throw new CommandException("EMPNO должен быть числом: \"" + empnoString + "\"");
					}
					st.setInt(1, empno);
					printEmployees(st.executeQuery());
				}
			}
			finally {
				if (st != null) st.close();
				if (conn != null) conn.close();
			}
		}
		catch (SQLException e) {
			throw new RuntimeException("SQLException: " + e.getMessage(), e);
		}
	}

	// Реализация команды insert-with-duplicate.
	// Демонстрация управления транзакциями.
	// Добавление двух связанных записи в таблицу Employee
	private static void executeInsertWithDuplicate(List<String> commandArgs)
		throws CommandException {

		if (commandArgs.size() != 3)
			throw new CommandException("Некорректное число аргументов");

		// считываем из аргументов значения для вставки
		String empnoString = commandArgs.get(0);
		int empno;
		try {
			empno = Integer.parseInt(empnoString);
		}
		catch (NumberFormatException e) {
			throw new CommandException("EMPNO должен быть числом: \"" + empnoString + "\"");
		}
		String ename = commandArgs.get(1);
		String jobTitle = commandArgs.get(2);

		// подключение драйвера Derby на случай, если используется не сановская JDK
		try {
			Class.forName(DERBY_DRIVER);
		}
		catch (ClassNotFoundException e) {
			throw new CommandException("Could not load Derby JDBC driver \"" + DERBY_DRIVER + "\"");
		}

		try {
			Connection conn = null;
			PreparedStatement st = null;
			try {
				conn = DriverManager.getConnection(CONNECTIONS_STRING);
				conn.setAutoCommit(false);

				st = conn.prepareStatement(
					"INSERT INTO Employee (EMPNO, ENAME, JOB_TITLE) " +
					"VALUES (?, ?, ?)" );
				st.setInt(1, empno);
				st.setString(2, ename);
				st.setString(3, jobTitle);
				st.executeUpdate();
				st.close();

				// проверка отката транзации:
				//int unused_ret = 1 / 0;

				// Демонстрация, что использование addBatch ортогонально использованию транзаций.
				st = conn.prepareStatement(
					"INSERT INTO Employee (EMPNO, ENAME, JOB_TITLE, DUPLICATE_EMPNO) " +
					"VALUES (?, ?, ?, ?)" );
				st.setInt(1, empno+1 );
				st.setString(2, ename);
				st.setString(3, jobTitle);
				st.setInt(4, empno);
				st.executeUpdate();

				conn.commit();
			}
			catch (SQLException e) {
				// Может возникнуть предположение, что этот вызов необязателен и
				// транзация должна откатиться при вызове close(), если ещё не
				// закоммичена. На самом деле, в случае наличия открытой
				// транзации close() выкидывает исключение.  Но при этом транзация,
				// вроде, всё же откатывается
				conn.rollback();
				throw e;
			}
			catch (RuntimeException e) {
				// Чтобы транзакция откатывалась в случае ошибки в коде
				conn.rollback();
				throw e;
			}
			finally {
				if (st != null) st.close();
				if (conn != null) conn.close();
			}
		}
		catch (SQLException e) {
			throw new RuntimeException("SQLException: " + e.getMessage(), e);
		}
	}

	// Печать записей из таблицы Employee из ResultSet.
	// ResultSet автоматически закрывается
	private static void printEmployees(ResultSet rs) throws SQLException {
		try {
			while (rs.next()) {
				int empno = rs.getInt("EMPNO");
				String ename = rs.getString("ENAME");
				String jobTitle = rs.getString("JOB_TITLE");
				System.out.println("" + empno + ", " + ename + ", " + jobTitle);
			}
		}
		finally {
			rs.close();
		}
	}
}

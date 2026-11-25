import java.io.*;
import java.util.*;

class User {
    String id;
    String name;

    User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String toString() {
        return id + " | " + name;
    }
}

class Message {
    String id;
    String senderId;
    String receiverId;
    String content;
    String time;

    Message(String id, String s, String r, String c, String t) {
        this.id = id;
        this.senderId = s;
        this.receiverId = r;
        this.content = c;
        this.time = t;
    }

    public String toString() {
        return "[" + time + "] " + senderId + " -> " + receiverId + ": " + content;
    }

    String toFile() {
        return id + "|" + senderId + "|" + receiverId + "|" + content + "|" + time;
    }
}

// ==================== DATA STRUCTURES ====================

// 1. LinkedList - Conversation history between two users
class ConversationLL {
    Node head;
    Node tail;
    int size;
    String u1;
    String u2;

    static class Node {
        Message m;
        Node next;

        Node(Message m) {
            this.m = m;
        }
    }

    ConversationLL(String a, String b) {
        u1 = a;
        u2 = b;
    }

    void add(Message m) {
        Node n = new Node(m);
        if (head == null) {
            head = tail = n;
        } else {
            tail.next = n;
            tail = n;
        }
        size++;
    }

    boolean between(String a, String b) {
        return (u1.equals(a) && u2.equals(b)) || (u1.equals(b) && u2.equals(a));
    }

    void show() {
        if (head == null) {
            System.out.println("  No messages");
            return;
        }
        Node c = head;
        int i = 1;
        while (c != null) {
            System.out.println("  " + i++ + ". " + c.m);
            c = c.next;
        }
    }

    List<Message> all() {
        List<Message> l = new ArrayList<>();
        Node c = head;
        while (c != null) {
            l.add(c.m);
            c = c.next;
        }
        return l;
    }
}

// 2. Queue - Outgoing messages (FIFO)
class OutgoingQueue {
    Node f;
    Node r;
    int sz;

    static class Node {
        Message m;
        Node next;

        Node(Message m) {
            this.m = m;
        }
    }

    void enq(Message m) {
        Node n = new Node(m);
        if (r == null) {
            f = r = n;
        } else {
            r.next = n;
            r = n;
        }
        sz++;
    }

    Message deq() {
        if (f == null) return null;
        Message m = f.m;
        f = f.next;
        if (f == null) r = null;
        sz--;
        return m;
    }

    boolean empty() {
        return sz == 0;
    }

    void show() {
        if (f == null) {
            System.out.println("  Empty");
            return;
        }
        Node c = f;
        int i = 1;
        while (c != null) {
            System.out.println("  " + i++ + ". " + c.m.id + " | " + c.m.content);
            c = c.next;
        }
    }

    Message removeLast() {
        if (f == null) return null;
        if (f == r) {
            Message m = f.m;
            f = r = null;
            sz--;
            return m;
        }
        Node c = f;
        while (c.next != r) {
            c = c.next;
        }
        Message m = r.m;
        c.next = null;
        r = c;
        sz--;
        return m;
    }
}

// 3. Stack - Undo last message (LIFO)
class UndoStack {
    Node top;
    int sz;

    static class Node {
        Message m;
        Node next;

        Node(Message m) {
            this.m = m;
        }
    }

    void push(Message m) {
        Node n = new Node(m);
        n.next = top;
        top = n;
        sz++;
    }

    Message pop() {
        if (top == null) return null;
        Message m = top.m;
        top = top.next;
        sz--;
        return m;
    }

    boolean empty() {
        return sz == 0;
    }
}

// 4. HashMap - User lookup O(1) with separate chaining
class UserMap {
    Entry[] t = new Entry[16];
    int n = 0;

    static class Entry {
        String k;
        User v;
        Entry next;

        Entry(String k, User v, Entry n) {
            this.k = k;
            this.v = v;
            this.next = n;
        }
    }

    int h(String k) {
        return Math.abs(k.hashCode()) % t.length;
    }

    void put(User u) {
        int i = h(u.id);
        Entry e = t[i];
        while (e != null) {
            if (e.k.equals(u.id)) {
                e.v = u;
                return;
            }
            e = e.next;
        }
        t[i] = new Entry(u.id, u, t[i]);
        if (++n > t.length * 0.75) {
            resize();
        }
    }

    User get(String id) {
        int i = h(id);
        Entry e = t[i];
        while (e != null) {
            if (e.k.equals(id)) return e.v;
            e = e.next;
        }
        return null;
    }

    boolean has(String id) {
        return get(id) != null;
    }

    void show() {
        for (Entry e : t) {
            while (e != null) {
                System.out.println("  " + e.v);
                e = e.next;
            }
        }
    }

    void resize() {
        Entry[] o = t;
        t = new Entry[t.length * 2];
        n = 0;
        for (Entry e : o) {
            while (e != null) {
                put(e.v);
                e = e.next;
            }
        }
    }
}

// ==================== MAIN APPLICATION ====================

public class ChatApp {

    // Data structures
    static UserMap users = new UserMap();
    static List<ConversationLL> convs = new ArrayList<>();
    static OutgoingQueue q = new OutgoingQueue();
    static UndoStack st = new UndoStack();
    static int cnt = 100;
    static final String FILE = "chat_messages.txt";

    public static void main(String[] args) {
        load();           // Load from file
        test();           // Run 4 test cases
        menu();           // Interactive menu
    }

    // ---- FILE HANDLING ----
    static void load() {
        // Default users
        users.put(new User("U001", "Aman"));
        users.put(new User("U002", "Riya"));
        users.put(new User("U003", "Karan"));
        users.put(new User("U004", "Priya"));

        File f = new File(FILE);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length < 5) continue;
                Message m = new Message(p[0], p[1], p[2], p[3], p[4]);
                getConv(p[1], p[2]).add(m);
                int x = Integer.parseInt(p[0].substring(1));
                if (x > cnt) cnt = x;
            }
        } catch (IOException e) {
            System.out.println("Load error: " + e.getMessage());
        }
    }

    static void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            for (ConversationLL c : convs) {
                for (Message m : c.all()) {
                    pw.println(m.toFile());
                }
            }
            System.out.println("Saved.");
        } catch (IOException e) {
            System.out.println("Save error: " + e.getMessage());
        }
    }

    // ---- HELPERS ----
    static ConversationLL getConv(String a, String b) {
        for (ConversationLL c : convs) {
            if (c.between(a, b)) return c;
        }
        ConversationLL n = new ConversationLL(a, b);
        convs.add(n);
        return n;
    }

    static ConversationLL findConv(String a, String b) {
        for (ConversationLL c : convs) {
            if (c.between(a, b)) return c;
        }
        return null;
    }

    // ---- CORE OPERATIONS ----
    static void send(String s, String r, String c) {
        if (!users.has(s)) {
            System.out.println("User " + s + " not found. Message not sent.");
            return;
        }
        if (!users.has(r)) {
            System.out.println("User " + r + " not found. Message not sent.");
            return;
        }
        Message m = new Message("M" + (++cnt), s, r, c, new Date().toString());
        q.enq(m);
        st.push(m);
        System.out.println("Message queued: " + m.id + " | " + users.get(s).name + " -> " + users.get(r).name);
    }

    static void deliver() {
        if (q.empty()) {
            System.out.println("No messages to deliver.");
            return;
        }
        Message m = q.deq();
        getConv(m.senderId, m.receiverId).add(m);
        System.out.println("Delivered: " + m.id + " | " + users.get(m.senderId).name + " -> " + users.get(m.receiverId).name);
    }

    static void view(String a, String b) {
        if (!users.has(a) || !users.has(b)) {
            System.out.println("User not found.");
            return;
        }
        ConversationLL c = findConv(a, b);
        System.out.println("--- " + users.get(a).name + " <-> " + users.get(b).name + " ---");
        if (c == null) c = new ConversationLL(a, b);
        c.show();
    }

    static void search(String id) {
        User u = users.get(id);
        System.out.println(u != null ? "Found: " + u : "User " + id + " not found.");
    }

    static void undo() {
        if (st.empty()) {
            System.out.println("No messages to undo.");
            return;
        }
        Message u = st.pop();
        Message r = q.removeLast();
        if (r != null && r.id.equals(u.id)) {
            System.out.println("Last message removed from outgoing queue/history.");
        } else if (r != null) {
            q.enq(r);
            System.out.println("Last message removed from history (already delivered).");
        } else {
            System.out.println("Last message removed from history.");
        }
    }

    // ---- TEST CASES (Auto-run on startup) ----
    static void test() {
        System.out.println("\n=== TEST CASES ===\n");

        System.out.println("TC1: Aman sends 'Hello' to Riya");
        send("U001", "U002", "Hello");
        System.out.println("Expected: Message queued: Mxxx | Aman -> Riya\n");

        System.out.println("TC2: Deliver when empty");
        deliver();
        deliver();
        System.out.println("Expected: No messages to deliver.\n");

        System.out.println("TC3: Send to unknown U999");
        send("U001", "U999", "test");
        System.out.println("Expected: User U999 not found. Message not sent.\n");

        System.out.println("TC4: Send 2, undo latest");
        send("U001", "U002", "First");
        send("U001", "U002", "Second");
        undo();
        System.out.println("Expected: Last message removed from outgoing queue/history.\n");
    }

    // ---- INTERACTIVE MENU ----
    static void menu() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- MENU ---");
            System.out.println("1. Send");
            System.out.println("2. Deliver");
            System.out.println("3. View Conversation");
            System.out.println("4. Search User");
            System.out.println("5. Undo Last");
            System.out.println("6. Show Queue");
            System.out.println("7. Show Stack");
            System.out.println("8. Show Users");
            System.out.println("9. Save & Exit");
            System.out.print("Choice: ");

            String choice = sc.nextLine();
            switch (choice) {
                case "1":
                    System.out.print("From: ");
                    String s = sc.nextLine();
                    System.out.print("To: ");
                    String r = sc.nextLine();
                    System.out.print("Msg: ");
                    send(s, r, sc.nextLine());
                    break;
                case "2":
                    deliver();
                    break;
                case "3":
                    System.out.print("U1: ");
                    String a = sc.nextLine();
                    System.out.print("U2: ");
                    view(a, sc.nextLine());
                    break;
                case "4":
                    System.out.print("ID: ");
                    search(sc.nextLine());
                    break;
                case "5":
                    undo();
                    break;
                case "6":
                    q.show();
                    break;
                case "7":
                    System.out.println("--- Stack ---");
                    if (st.empty()) {
                        System.out.println("  Empty");
                    } else {
                        UndoStack.Node c = st.top;
                        int i = 1;
                        while (c != null) {
                            System.out.println("  " + i++ + ". " + c.m.id + " | " + c.m.content);
                            c = c.next;
                        }
                    }
                    break;
                case "8":
                    users.show();
                    break;
                case "9":
                    save();
                    System.out.println("Bye!");
                    sc.close();
                    return;
                default:
                    System.out.println("Invalid.");
            }
        }
    }
}
package dtos;

public class ConventusResourceDTO {
    private String id;
    private String text;
    private String start;
    private String end;

    public ConventusResourceDTO(String id, String text, String start, String end) {
        this.id = id;
        this.text = text;
        this.start = start;
        this.end = end;
    }

    public String getText() {
        return text;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }
}

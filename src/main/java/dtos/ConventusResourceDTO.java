package dtos;

public class ConventusResourceDTO {
    private String text;
    private String start;
    private String end;

    public ConventusResourceDTO(String text, String start, String end) {
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

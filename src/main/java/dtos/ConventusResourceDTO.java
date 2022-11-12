package dtos;

public class ConventusResourceDTO {
    private String id;
    private String text;
    private String start;
    private String end;
    private String backColor;

    public ConventusResourceDTO(String id, String text, String start, String end, String backColor) {
        this.id = id;
        this.text = text;
        this.start = start;
        this.end = end;
        this.backColor = backColor;
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

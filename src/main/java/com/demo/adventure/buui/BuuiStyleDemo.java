package com.demo.adventure.buui;

public final class BuuiStyleDemo {

    private BuuiStyleDemo() {
    }

    public static void main(String[] args) {
        String markdown = """
# Next steps:

1. **Restart** the API and re-run the same request.
2. _Share_ logs if it still fails.

> Blockquote: use _italics_ for callouts.

```
code block example
```

***

Table:

| Item | Status |
| --- | --- |
| Build | **Passing** |
| Deploy | _Queued_ |

List:
- Bullet
- **Bold** and _italic_
""";

        System.out.println(MarkdownRenderer.render(markdown, 72, 0));
    }
}

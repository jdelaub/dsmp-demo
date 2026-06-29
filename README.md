# DSM Pro — Demo Pages

Static pages for the **Demo Session Manager Pro** guided walkthrough. Each
page is opened by a session in the extension's "DSM Pro Demo" profile
(Settings → Reset to sample data → "Guided demo (walkthrough)").

## Hosting (GitHub Pages)

1. **Settings → Pages → Deploy from a branch → `main` / `(root)`**.
2. Pages serves every file at `https://jdelaub.github.io/dsmp-demo/<file>`.

All six pages are blank canvases whose background steps from pure white to
DSMP blue (`#3b82f6`); the persona badge + visual indicator are injected by
the extension, so each page shows a different persona/colour combination
against a progressively bluer background.

| Page | Background | Persona / session |
|------|-----------|-------------------|
| `01-welcome.html` | `#ffffff` (white) | Maya · Sales Lead — red indicator |
| `02-shade.html` | `#d8e6fd` | Leo · Solutions Engineer — amber |
| `03-shade.html` | `#b1cdfb` | Aisha · Customer Success — emerald |
| `04-shade.html` | `#89b4fa` | Noah · Product Demo — violet |
| `05-shade.html` | `#629bf8` | Sofia · Account Exec — pink |
| `06-dsmp-blue.html` | `#3b82f6` (DSMP blue) | Ethan · Support — blue |

Live at `https://jdelaub.github.io/dsmp-demo/<file>`.

## Adding a page

Drop a new `NN-name.html` at the repo root and add a matching session to
`extension/assets/sample-data-tutorial.json` in the extension repo, with
`"url": "https://jdelaub.github.io/dsmp-demo/NN-name.html"`.

> Keep this repo limited to demo pages only — it is published publicly by
> GitHub Pages.

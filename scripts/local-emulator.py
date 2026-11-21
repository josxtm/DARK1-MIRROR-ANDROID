"""Hide upstream install and hardware connection controls in Android's offline demo."""
import re
import sys
from pathlib import Path

path = Path(sys.argv[1])
html = path.read_text()
html = re.sub(r'<link rel="(?:preconnect|stylesheet)" href="https://fonts\.[^>]+>\n', '', html)
html = re.sub(
    r'if \(navigator\.permissions && navigator\.requestMIDIAccess\)[^\n]*\n'
    r'  navigator\.permissions\.query\([^\n]*\n',
    '',
    html,
)
style = '<style>#midi, header nav, .turn { display: none !important; }</style>'
html = html.replace('</head>', style + '\n</head>')
path.write_text(html)

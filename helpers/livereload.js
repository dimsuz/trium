var fs = require('fs');

fs.watch('./target', [], function(event, filename) {
  if (location && /\.js$/.test(filename))
    location.reload(false);
});
fs.watch('./main.js', [], function(event, filename) {
  if (location && /\.js$/.test(filename))
    location.reload(false);
});
fs.watch('./css/trium.css', [], function(event, filename) {
  if (location && /\.css$/.test(filename))
    location.reload(false);
});
fs.watch('index.html', [], function(event, filename) {
  if (location && /\.html$/.test(filename))
    location.reload(false);
});

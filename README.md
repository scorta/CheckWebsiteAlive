# CheckWebsiteAlive
This program will help you to check if a url from a list is dead or alive.
**_Note:_** this program should be called "Check**Url**Alive"

## Usage
Compile this, and run with something like:
`$ java -jar CheckWebsiteAlive list_url dead_phrases 42 0`

Those 4 params:
- file name that contains list of urls
- file name that contains list of dead phrases
- number of thread(s)
- and choice (0 == checking url alive, 1 == get page source)

The result will be written to a file name `list_url` + postfix `_out.txt` in format:

`url \t status (live / dead)`

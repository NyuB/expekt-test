import sys
import unittest


def changelog_lines(lines: list[str]) -> list[str]:
    if len(lines) < 1 or not lines[0].startswith("# "):
        return []
    res = []
    for line in lines[1:]:
        if line.startswith("# "):
            break
        res.append(line)
    return res


if __name__ == "__main__":
    with open(sys.argv[1], "r") as f:
        changelog = f.read().split("\n")
    for l in changelog_lines(changelog):
        print(l)


class Tests(unittest.TestCase):
    def test_only_one_line(self):
        self.assertEqual(changelog_lines(["# Current", "One line"]), ["One line"])

    def test_subsections(self):
        self.assertEqual(
            changelog_lines(["# Current", "## Bugfixes", "Oops"]),
            ["## Bugfixes", "Oops"],
        )

    def test_stop_before_next_section(self):
        self.assertEqual(
            changelog_lines(["# Current", "Kept", "# 1.0.0", "Skipped"]), ["Kept"]
        )

import unittest
import sys
import re

semver = re.compile(r"[0-9]+[.][0-9]+[.][0-9]+(-RC[0-9]+)?")


def ok(version: str) -> bool:
    return semver.fullmatch(version) is not None


def main(version: str):
    if ok(version):
        sys.exit(0)
    else:
        print(f"'{version}' does not comply to semantic versioning")
        sys.exit(1)


if __name__ == "__main__":
    main(sys.argv[1])


class Tests(unittest.TestCase):
    def ok(self, version: str):
        self.assertTrue(ok(version))

    def ko(self, version: str):
        self.assertFalse(ok(version))

    def test_nominal(self):
        for version in [
            "1.2.3",
            "12.3.4",
            "1.23.4",
            "1.2.34",
        ]:
            with self.subTest(version):
                self.ok(version)

    def test_rc(self):
        self.ok("1.2.3-RC0")

    def test_rc_no_number(self):
        self.ko("1.2.3-RC")

    def test_wrong_prefix(self):
        self.ko("oops1.0.0")

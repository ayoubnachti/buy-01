import base64
import json
import os
import unittest
from collections import Counter
from datetime import datetime, timezone
from unittest.mock import patch

import seed_demo_data


class SeedPlanTests(unittest.TestCase):
    def test_product_catalog_has_thirty_products_in_five_even_categories(self):
        products = seed_demo_data.product_specs()

        self.assertEqual(len(products), 30)
        self.assertEqual(
            Counter(product["category"] for product in products),
            {category: 6 for category, _ in seed_demo_data.CATEGORIES},
        )
        self.assertEqual(len({product["name"] for product in products}), 30)
        self.assertEqual(Counter(product["seller_index"] for product in products), {0: 15, 1: 15})

    def test_order_plan_has_forty_orders_evenly_distributed_across_statuses(self):
        anchor = datetime(2026, 10, 31, 12, 0, tzinfo=timezone.utc)
        plan = seed_demo_data.order_plan(anchor)

        self.assertEqual(len(plan), 40)
        self.assertEqual(
            Counter(order["status"] for order in plan),
            {status: 8 for status in seed_demo_data.ORDER_STATUSES},
        )
        for order in plan:
            age_days = (anchor - order["created_at"]).total_seconds() / 86400
            self.assertGreater(age_days, 0)
            self.assertLessEqual(age_days, 60)

    def test_status_history_uses_valid_progression_and_actors(self):
        created_at = datetime(2026, 10, 1, tzinfo=timezone.utc)

        history = seed_demo_data.status_history(
            "DELIVERED", created_at, "client-id", "seller-id"
        )

        self.assertEqual(
            [entry["status"] for entry in history],
            ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED"],
        )
        self.assertEqual(
            [entry["by"] for entry in history],
            ["client-id", "seller-id", "seller-id", "seller-id"],
        )

    def test_jwt_claims_are_decoded_without_logging_token(self):
        payload = base64.urlsafe_b64encode(
            json.dumps({"userId": "client-id", "role": "CLIENT"}).encode()
        ).decode().rstrip("=")

        claims = seed_demo_data.jwt_claims(f"header.{payload}.signature")

        self.assertEqual(claims, {"userId": "client-id", "role": "CLIENT"})

    def test_api_response_helpers_validate_object_and_list_data_shapes(self):
        self.assertEqual(seed_demo_data.response_data({"data": {"id": "p1"}}), {"id": "p1"})
        self.assertEqual(seed_demo_data.response_list({"data": [{"id": "o1"}]}), [{"id": "o1"}])
        with self.assertRaises(seed_demo_data.SeedError):
            seed_demo_data.response_list({"data": {"id": "o1"}})

    def test_api_client_refuses_remote_hosts_and_non_dev_http(self):
        with patch.dict(os.environ, {"DEMO_ACTIVE_PROFILE": "dev"}):
            with self.assertRaises(seed_demo_data.SeedError):
                seed_demo_data.ApiClient("https://example.com")

        with patch.dict(os.environ, {"DEMO_ACTIVE_PROFILE": "staging"}):
            with self.assertRaises(seed_demo_data.SeedError):
                seed_demo_data.ApiClient("http://localhost:8080")

    @patch("seed_demo_data.run_mongo_script")
    def test_reset_removes_previous_orders_only_for_reserved_demo_clients(self, run_mongo):
        seed_demo_data.reset_demo_orders(["client-one", "client-two"])

        script = run_mongo.call_args.args[0]
        self.assertIn('getSiblingDB("orders_db")', script)
        self.assertIn('"client-one","client-two"', script)
        self.assertIn("deleteMany", script)


if __name__ == "__main__":
    unittest.main()
